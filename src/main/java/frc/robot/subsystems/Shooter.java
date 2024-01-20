// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

import com.ctre.phoenix6.controls.Follower;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.ControlModeValue;
import com.ctre.phoenix6.signals.NeutralModeValue;

import edu.wpi.first.wpilibj2.command.SubsystemBase;

public class Shooter extends SubsystemBase {

  TalonFX forwardTop;
  TalonFX forwardBottom;
  TalonFX middle;

  /** Creates a new Shooter. */
  public Shooter() {

    forwardTop = new TalonFX(0);
    forwardBottom = new TalonFX(0);
    middle = new TalonFX(0);

    forwardTop.setNeutralMode(NeutralModeValue.Coast);
    forwardBottom.setNeutralMode(NeutralModeValue.Coast);
    middle.setNeutralMode(NeutralModeValue.Coast);

    forwardBottom.setInverted(true);
  }

  public void setShooterSpeed(double percentOutput) {
    forwardTop.set(percentOutput);
    forwardBottom.set(percentOutput);
    middle.set(percentOutput);
  }


  @Override
  public void periodic() {
    // This method will be called once per scheduler run
  }
}
