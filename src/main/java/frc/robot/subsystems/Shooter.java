// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

import javax.swing.text.StyleContext.SmallAttributeSet;

import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.controls.DutyCycleOut;
import com.ctre.phoenix6.controls.Follower;
import com.ctre.phoenix6.controls.MotionMagicVelocityVoltage;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.ControlModeValue;
import com.ctre.phoenix6.signals.NeutralModeValue;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.BangBangController;
import edu.wpi.first.wpilibj.motorcontrol.Talon;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

public class Shooter extends SubsystemBase {

  TalonFX top;
  TalonFX bottom;
  static final double unitsPerRot = 4096;
  BangBangController flyWheelController = new BangBangController();
  private final double MAXSPEED = 500;
  /** Creates a new Shooter. */
  public Shooter() {
    TalonFXConfiguration configuration = new TalonFXConfiguration();
    // configuration.motionAcceleration = 
    top = new TalonFX(40);
    bottom = new TalonFX(41);
    
    top.setNeutralMode(NeutralModeValue.Coast);
    bottom.setNeutralMode(NeutralModeValue.Coast);
  //   top.config_kP(0, 0.000);
  //   top.config_kI(0, 0.000);
  //   top.config_kD(0, 0.00);
  // top.config_kF(0, 0.02046)itop\\
    // top.setInverted(true);
    //     bottom.setInverted(true);

    var slot0Configs = configuration.Slot0;
    slot0Configs.kS = 0.2;
    slot0Configs.kV = 0.18;
    slot0Configs.kA = 0.1;
    slot0Configs.kP = 0.11;
    slot0Configs.kI = 0;
    slot0Configs.kD = 0;

    var motionMagic = configuration.MotionMagic;

    motionMagic.MotionMagicAcceleration = 200;
    motionMagic.MotionMagicJerk = 2000;

    top.getConfigurator().apply(configuration);
    bottom.getConfigurator().apply(configuration);


    top.setInverted(true);
    bottom.setControl(new Follower(40, false));
    
  }

  public double getVelocity(){
    return top.getVelocity().getValueAsDouble();
  }



  public void setShooterRPM(double percentOutput){
    // double error = RPM - top.getSelectedSensorVelocity();
    // Multiply velocity units by 600/UnitsPerRotation to obtain RPM.

    // double Velo = RPM * unitsPerRot/600;
    double vel = getVelocity();
    if (Math.abs(vel) < 5 ) {
      vel = 0;
    }
    double output = flyWheelController.calculate(vel, percentOutput*MAXSPEED);
    // SmartDashboard.putNumber("outputPercemtFlyWheel", output);
    top.setControl(new DutyCycleOut(output));

  }
  public void setPower(double power){
    top.setControl(new DutyCycleOut(power));
  }

  public void setRPS(double RPS){
    top.setControl(new MotionMagicVelocityVoltage(RPS));
  }
 

  @Override
  public void periodic() {
    // This method will be called once per scheduler run
  }
}
