// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.commands;

// import java.util.Ti/mer;
import java.util.TimerTask;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

import com.ctre.phoenix6.mechanisms.swerve.SwerveDrivetrain;
import com.ctre.phoenix6.mechanisms.swerve.SwerveModule.DriveRequestType;
import com.ctre.phoenix6.mechanisms.swerve.SwerveRequest;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.generated.TunerConstants;
import frc.robot.subsystems.Arm;
import frc.robot.subsystems.CommandSwerveDrivetrain;
import frc.robot.subsystems.Intake;
import frc.robot.subsystems.Leds;
import frc.robot.subsystems.LimelightHelpers;
import frc.robot.subsystems.Shooter;
import frc.robot.subsystems.Leds.SparkLEDColors;


public class SuperStructure extends Command {
  private double MaxSpeed = 6; // 6 meters per second desired top speed
  private double MaxAngularRate = 1.5 * Math.PI; // 3/4 of a rotation per second max angular velocity


  private final CommandSwerveDrivetrain drivetrain = TunerConstants.DriveTrain; // My drivetrain
 private final SwerveRequest.FieldCentric drive = new SwerveRequest.FieldCentric()
      .withDeadband(MaxSpeed * 0.1).withRotationalDeadband(MaxAngularRate * 0.1) // Add a 10% deadband
      .withDriveRequestType(DriveRequestType.OpenLoopVoltage); // I want field-centric
                                                               // driving in open loop
  private Supplier<Double> driveTrainSupplier;
  private Arm arm;
  private Intake intake;
  private Leds ledSub;
  private Shooter shoot;
  private ArmPosition currentPosition = ArmPosition.STOW;
  private IntakeMode currentIntake = IntakeMode.MANUAL;
  private boolean cancelAlgoShoot = false;

  private XboxController operator;
  private Timer timer = new Timer();
  private java.util.Timer intakeTimer = new java.util.Timer();
  private java.util.Timer autoTippingTimer = new java.util.Timer();
  private XboxController driver;

  public enum IntakeMode {
    OUTTAKE(1, 1000), // for algo shoot and amp shoot
    INTAKE(0.5, 750), // for high intake shoot
    MANUAL(0, 1000);

    private double speed;
    private long time; // IN MS

    IntakeMode(double speed, long time) {
      this.speed = speed;
      this.time = time;
    }

    public double getSpeed() {
      return this.speed;
    }
     public long getTime() {
      return this.time;
    }
  }

  public enum ArmPosition {
    REVERSE_TIPPING(-0.3),
    STOW(-0.25),
    LOW_INTAKE(0.06),
    HIGH_INTAKE(-0.15),
    AMP(0.01),
    SHOOT(-0.02),
    STAGEFIT(0.01),
    ALGO(0),
    HORIZONTAL(0);

    private double pos;

    ArmPosition(double pos) {
      this.pos = pos;
    }

    public double getPos() {
      return this.pos;
    }
  }

  /** Creates a new SuperStructure. */
  public SuperStructure(Arm arm, Intake intake, Shooter shoot, Leds led, XboxController driver, XboxController operator) {
    // Use addRequirements() here to declare subsystem dependencies.
    this.arm = arm;
    this.intake = intake;
    this.shoot = shoot;
    this.ledSub = led;

    this.driver = driver;

    this.operator = operator;

    driveTrainSupplier = () -> (Math.signum(driver.getRightX())
                * -(Math.abs(driver.getRightX()) > 0.15 ? Math.abs(Math.pow(driver.getRightX(), 2)) + 0.1 : 0))
                * MaxAngularRate;

    addRequirements(arm);
    addRequirements(intake);
    addRequirements(shoot);
  }

  // Called when the command is initially scheduled.
  @Override
  public void initialize() {
    currentPosition = ArmPosition.STOW;
    currentIntake = IntakeMode.MANUAL;
    ledSub.setLED(SparkLEDColors.RAINBOW);

// Sets up Swerve
    drivetrain.setDefaultCommand( // Drivetrain will execute this command periodically
        drivetrain.applyRequest(() -> drive
            .withVelocityX((Math.signum(driver.getLeftY())
                * -(Math.abs(driver.getLeftY()) > 0.1 ? Math.abs(Math.pow(driver.getLeftY(), 2)) + 0.1 : 0))
                * MaxSpeed) // Drive forward with
            // negative Y (forward)
            .withVelocityY((Math.signum(driver.getLeftX())
                * -(Math.abs(driver.getLeftX()) > 0.1 ? Math.abs(Math.pow(driver.getLeftX(), 2)) + 0.1 : 0))
                * MaxSpeed) // Drive left with negative X (left)
            .withRotationalRate(driveTrainSupplier.get()) // Drive counterclockwise with negative X (left)
        ));
  }

  // Called every time the scheduler runs while the command is scheduled.
  @Override
  public void execute() {
    SmartDashboard.putNumber("AP", arm.getArmPosition());
    SmartDashboard.updateValues();
   
    if (operator.getRightBumperPressed()) {
      cancelAlgoShoot = false;
      setPosition(ArmPosition.ALGO);
      ledSub.setLED(SparkLEDColors.ALGO_AIM);
      driveTrainSupplier = () -> Vision.aimLimelightAprilTags() * MaxAngularRate;
    } 
    else if (operator.getRightBumperReleased()) {
      if (!cancelAlgoShoot) {
        setIntakeMode(IntakeMode.OUTTAKE);
        ledSub.setLED(SparkLEDColors.ALGO_SHOOT);
        

      }
         driveTrainSupplier = () -> (Math.signum(driver.getRightX())
                * -(Math.abs(driver.getRightX()) > 0.15 ? Math.abs(Math.pow(driver.getRightX(), 2)) + 0.1 : 0))
                * MaxAngularRate;
    }
    else if (operator.getXButton()) {
      // Cancel button for algo shoot
      if (currentPosition == ArmPosition.ALGO) {
        cancelAlgoShoot = true;
        setPosition(ArmPosition.STOW);
      }
    }
    else if (operator.getAButton()) {
      
      setPosition(ArmPosition.LOW_INTAKE);
      ledSub.setLED(SparkLEDColors.LOW_INTAKE);
    }
    else if (operator.getStartButton()) {
      // Tipping algo
      // autoPickBackUpAlgo();
    }
    else if (operator.getBButton()) {
      setPosition(ArmPosition.AMP);
      ledSub.setLED(SparkLEDColors.AMP);

      
    } else if (operator.getYButton()) {
      setPosition(ArmPosition.STOW);
      setIntakeMode(IntakeMode.MANUAL);
      ledSub.setLED(SparkLEDColors.RAINBOW);


    } else if (operator.getLeftBumper()) {
      setPosition(ArmPosition.HIGH_INTAKE);
      setIntakeMode(IntakeMode.INTAKE);
      ledSub.setLED(SparkLEDColors.HIGH_INTAKE);


    }
    else if (operator.getPOV() == 270) {
      setPosition(ArmPosition.STAGEFIT);

    }
    else if (MathUtil.applyDeadband(operator.getRightY(), 0.1) != 0) {
      // Override Intake mode at any point to be manual
      currentIntake = IntakeMode.MANUAL;
    }
    else {
      // Should be running continously
      // Balancing algo
      // autoBalancingAlgo();
    }


    if(operator.getPOV() == 180){
      intake.useBanner = !intake.useBanner;
    }


    if (currentPosition == ArmPosition.ALGO) { // removed && operator.rightBumper()
      shoot.motionMagicVelo(NetworkTableInstance.getDefault().getTable("shootModel").getEntry("predictedPerOut").getDouble(0));
      arm.setPosition(NetworkTableInstance.getDefault().getTable("shootModel").getEntry("predictedTheta")
          .getDouble(ArmPosition.HIGH_INTAKE.getPos()));
    } 
    else {
     if (currentPosition == ArmPosition.AMP) {
      shoot.motionMagicVelo(25, 13);
     }
     else {
      shoot.motionMagicVelo(0, 0);
     }

      arm.setPosition(currentPosition.getPos());
          // arm.setPosition(arm.getArmPosition() - MathUtil.applyDeadband(operator.getLeftY() / 10, 0.005));

    }    
    SmartDashboard.putNumber("pitch gyro", arm.getGyroPitch());

    SmartDashboard.putNumber("ty", LimelightHelpers.getTY("limelight"));
     SmartDashboard.putNumber("Algo shoot Output", NetworkTableInstance.getDefault().getTable("shootModel").getEntry("predictedPerOut").getDouble(0));
      // shoot.motionMagicVelo(
          // );
          SmartDashboard.putBoolean("bnanner", intake.getBanner());
                SmartDashboard.putNumber("Algo shoot theta", NetworkTableInstance.getDefault().getTable("shootModel").getEntry("predictedTheta").getDouble(0));
      SmartDashboard.updateValues();

    if (currentIntake == IntakeMode.MANUAL) {
      // intake.runIntake(MathUtil.applyDeadband(operator.getRightY(), 0.1));
      intake.pushIntake(operator.getRightY() * 0.75);

    }
    else if (currentIntake == IntakeMode.INTAKE) {
      intake.runIntake(currentIntake.getSpeed());
    }
    else if (currentIntake == IntakeMode.OUTTAKE) {

      intake.pushIntake(currentIntake.getSpeed());
    }
  }

  public void setPosition(ArmPosition pos) {
    this.currentPosition = pos;
  }
 


  public void setIntakeMode(IntakeMode mode) {

    // If mode isn't manual
    if (mode != IntakeMode.MANUAL) {
      // if the arm is in algo mode
      if (currentPosition == ArmPosition.ALGO) {
        // We can immediately shoot it
        currentIntake = mode;
        intakeTimer.purge();
        intakeTimer.schedule(new TimerTask() {
            @Override
            public void run() {
              // Sets intake mode back to manual
              currentIntake = IntakeMode.MANUAL;
              setPosition(ArmPosition.STOW); // Moves it to Stow
              ledSub.setLED(SparkLEDColors.RAINBOW);
              this.cancel();
            }
          }, mode.getTime());
      }
      else {
        // This is if it is not in algo mode
        // First continously check if the arm is at the setpoint
        // intakeTimer.cancel();
        intakeTimer.purge();
        intakeTimer.scheduleAtFixedRate(new TimerTask() {
          @Override
          public void run() {
            if (arm.isArmInRange(currentPosition)) {
              // Once it is, we can cancel this timer and set the currentIntake mode to the specified mode, and schedule
              // a new comman that is supposed to run the intake till the time expires, or banner sensor is triggered
              
              this.cancel();

              currentIntake = mode;

              intakeTimer.schedule(new TimerTask() {
                  @Override
                  public void run() {
                    // Sets intake mode back to manual

                    currentIntake = IntakeMode.MANUAL;

                    this.cancel();
                  }
                }, mode.getTime());
                  
            }
          }
        }, 0, 1);      
      }
    }
    else {
      this.currentIntake = mode;
    }
  }

  public void autoBalancingAlgo() {
    double pitch = arm.getGyroPitch();
    if (Math.abs(pitch) > 25) {
      currentPosition = Math.signum(pitch) > 0 ? ArmPosition.REVERSE_TIPPING : ArmPosition.HIGH_INTAKE;
    }
  }
  public void autoPickBackUpAlgo() {
    

    currentPosition = ArmPosition.STOW;
    autoTippingTimer.purge();
    
    autoTippingTimer.scheduleAtFixedRate(new TimerTask() {
        @Override
        public void run() {
          if (arm.isArmInRange(currentPosition)) {
            // Once it is, we can cancel this timer and set the currentIntake mode to the specified mode, and schedule
            // a new comman that is supposed to run the intake till the time expires, or banner sensor is triggered
            
            this.cancel();

            double pitch = arm.getGyroPitch();
            if (pitch > 0) {
              currentPosition = ArmPosition.LOW_INTAKE;
            }
            else {
              currentPosition = ArmPosition.REVERSE_TIPPING;
            }
            
            this.cancel();
        
          }
        }
      }, 0, 1);      

    
  }

  
  // Called once the command ends or is interrupted.
  @Override
  public void end(boolean interrupted) {}

  // Returns true when the command should end.
  @Override
  public boolean isFinished() {
    return false;
  }
}
