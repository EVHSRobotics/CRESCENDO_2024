// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.commands;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;

import org.opencv.core.Mat;

import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.cscore.CvSink;
import edu.wpi.first.cscore.CvSource;
import edu.wpi.first.cscore.UsbCamera;
import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.AddressableLED;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import frc.robot.subsystems.Limelight;
import frc.robot.subsystems.LimelightHelpers;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;


public class Vision extends Command {

  // private AprilScanner aprilScanner;
  private Limelight aprilLimelight;
  private CommandXboxController xboxController;
  public Limelight gameObjectTopLimelight;
  public Limelight gameObjectBottomLimelight;
  public double xdistance;
  NetworkTable table1;
  NetworkTableEntry tx, tv;

  double errorsum = 0;
  double lasterror = 0;
  double error;
  double lastTimestamp = 0;

  

  public Vision() {
    // Use addRequirements() here to declare subsystem dependencies.
    // VideoServer videoServer, AprilScanner aprilScanner,
    // this.aprilScanner = aprilScanner;
    // table1 = NetworkTableInstance.getDefault().getTable("limelight-bottom");
    aprilLimelight = new Limelight(1);
    // addRequirements(videoServer);
    // addRequirements(aprilScanner);
   SmartDashboard.putString("H", "Hello");
    SmartDashboard.updateValues();
   
  }

  // Called when the command is initially scheduled.
  @Override
  public void initialize() {

   
  }


  public double getAimRotation() {
    double distance = aprilLimelight.tx.getDouble(0);
    double error = 0.1; 
    double p_constant = -0.028; 
    double output = (distance - error) * p_constant;
    SmartDashboard.putNumber("distance", distance);
    SmartDashboard.putNumber("output", output);
    SmartDashboard.updateValues();
    return output;

  }

  // Called every time the scheduler runs while the command is scheduled.
  @Override
  public void execute() {
    
   
  }

 


  
  public void aimLimelightAprilTags() {

    double x = aprilLimelight.getX();
    errorsum = 0;
    error = x;

    double dt = Timer.getFPGATimestamp() - lastTimestamp;
    double errorrate = (error-lasterror)/dt;
    if(Math.abs(x) < 0.1){
        errorsum += dt *  x;
    }
    double output = MathUtil.clamp(error*0.045 + errorrate *0+errorsum*0.0, -1, 1);

    SmartDashboard.putNumber("limelight", ( output));
    SmartDashboard.updateValues();
    lastTimestamp = Timer.getFPGATimestamp();
    lasterror = error;
    
 
  }
  

  // Called once the command ends or is interrupted.
  @Override
  public void end(boolean interrupted) {
  }

  // Returns true when the command should end.
  @Override
  public boolean isFinished() {
    return false;
  }
}