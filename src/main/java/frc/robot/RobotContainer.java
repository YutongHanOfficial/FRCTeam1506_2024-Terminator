// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import java.util.List;

import com.ctre.phoenix6.Utils;
import com.ctre.phoenix6.configs.CurrentLimitsConfigs;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.hardware.Pigeon2;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.mechanisms.swerve.SwerveRequest;
import com.pathplanner.lib.auto.NamedCommands;
import com.pathplanner.lib.path.GoalEndState;
import com.pathplanner.lib.path.PathConstraints;
import com.pathplanner.lib.path.PathPlannerPath;
import com.ctre.phoenix6.mechanisms.swerve.SwerveModule.DriveRequestType;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.wpilibj.PS4Controller;
import edu.wpi.first.wpilibj.PneumaticHub;
import edu.wpi.first.wpilibj.PneumaticsModuleType;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.RepeatCommand;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import edu.wpi.first.wpilibj2.command.button.JoystickButton;
import edu.wpi.first.wpilibj2.command.button.POVButton;
import frc.robot.subsystems.Angler;
import frc.robot.subsystems.Climber;
import frc.robot.subsystems.Vision;
import frc.robot.Constants.IntakeSubsystem;
import frc.robot.Constants.ShooterSubsystem;
import frc.robot.commands.intake.intake;
import frc.robot.commands.shooter.angle;
import frc.robot.commands.shooter.moveAngler;
import frc.robot.commands.shooter.shoot;
import frc.robot.commands.vision.*;
import frc.robot.generated.TunerConstants;

public class RobotContainer {

  private double MaxSpeed = 5; // 6 meters per second desired top speed
  private double MaxAngularRate = 1.5 * Math.PI; // 3/4 of a rotation per second max angular velocity

  /* Setting up bindings for necessary control of the swerve drive platform */
  private final PS4Controller driver = new PS4Controller(0); // My joystick
  private final PS4Controller operator = new PS4Controller(1); // My joystick

  //Subsystems!!!!
  public final CommandSwerveDrivetrain drivetrain = TunerConstants.DriveTrain; // My drivetrain
  public final Vision vision = new Vision();
  public final frc.robot.subsystems.IntakeSubsystem intake = new frc.robot.subsystems.IntakeSubsystem();
  public final frc.robot.subsystems.ShooterSubsystem shooter = new frc.robot.subsystems.ShooterSubsystem(vision);
  public final Angler angler = new Angler();
  public final Autos autos = new Autos(intake, shooter, angler, vision);
  public final Climber climber = new Climber();

  //Commands
  vision2 visionCommand = new vision2(vision);

  private final SwerveRequest.FieldCentric drive = new SwerveRequest.FieldCentric()
      .withDeadband(MaxSpeed * 0.1).withRotationalDeadband(MaxAngularRate * 0.1) // Add a 10% deadband
      .withDriveRequestType(DriveRequestType.OpenLoopVoltage); // I want field-centric
                                                               // driving in open loop
  private final SwerveRequest.SwerveDriveBrake brake = new SwerveRequest.SwerveDriveBrake();
  private final SwerveRequest.RobotCentric forwardStraight = new SwerveRequest.RobotCentric().withDriveRequestType(DriveRequestType.OpenLoopVoltage);
  private final SwerveRequest.PointWheelsAt point = new SwerveRequest.PointWheelsAt();

  /* Path follower */

  // private final Telemetry logger = new Telemetry(MaxSpeed);
  // private final Field2d m_field = Constants.Swerve.m_field;

  private void configureBindings() {
    drivetrain.setDefaultCommand( // Drivetrain will execute this command periodically
        drivetrain.applyRequest(() -> drive.withVelocityX(-driver.getLeftY() * MaxSpeed) // Drive forward with
                                                                                           // negative Y (forward)
            .withVelocityY(-driver.getLeftX() * MaxSpeed) // Drive left with negative X (left)
            .withRotationalRate(-driver.getRightX() * MaxAngularRate) // Drive counterclockwise with negative X (left)
        ).ignoringDisable(true));

    j.dA.whileTrue(drivetrain.applyRequest(() -> brake));

    // reset the field-centric heading on left bumper press
    j.dB.onTrue(drivetrain.runOnce(() -> drivetrain.seedFieldRelative()));
    // j.dY.onTrue(drivetrain.applyRequest(() -> brake));

    if (Utils.isSimulation()) {
      drivetrain.seedFieldRelative(new Pose2d(new Translation2d(), Rotation2d.fromDegrees(90)));
    }
    // drivetrain.registerTelemetry(logger::telemeterize);

    j.dUp.whileTrue(drivetrain.applyRequest(() -> forwardStraight.withVelocityX(0.5).withVelocityY(0)));
    j.dDown.whileTrue(drivetrain.applyRequest(() -> forwardStraight.withVelocityX(-0.5).withVelocityY(0)));
    
    // j.dLeft.whileTrue(new RepeatCommand(new vision2(vision))); //shooter alignment
    j.dLeft.whileTrue(new vision2(vision).until(() -> vision.x > -Constants.Limelight.shooterThreshold && vision.x < Constants.Limelight.shooterThreshold)); //shooter alignment
    j.dRight.whileTrue(new kevin2(vision)); //intake alignment

    RepeatCommand repeat = new RepeatCommand(new InstantCommand(() -> intake.intake()));
    // j.oRT.whileTrue(new InstantCommand(() -> intake.intake()));
    j.oRT.whileTrue(repeat);
    j.oRT.whileFalse(new InstantCommand(() -> intake.stopIntakeWithReset()));
    j.oLT.whileTrue(new InstantCommand(() -> intake.outtake()));
    j.oLT.whileFalse(new InstantCommand(() -> intake.stopIntake()));

    j.oRB.whileTrue(new InstantCommand(() -> shooter.shootMax()));
    j.oRB.whileFalse(new InstantCommand(() -> shooter.shootStop()));

    j.oUp.whileTrue(new InstantCommand(() -> angler.anglerUp()));
    j.oDown.whileTrue(new InstantCommand(() -> angler.anglerDown()));
    j.oTouchpad.whileTrue(new angle(angler, 0.58));
    j.oA.whileTrue(new InstantCommand(() -> angler.anglerZero()));
    j.oUp.whileFalse(new InstantCommand(() -> angler.stopAngler()));
    j.oDown.whileFalse(new InstantCommand(() -> angler.stopAngler()));
    j.oTouchpad.whileFalse(new InstantCommand(() -> angler.stopAngler()));

    j.oA.whileTrue(new InstantCommand(() -> shooter.increaseIncrement()));
    j.oY.whileTrue(new InstantCommand(() -> shooter.decreaseIncrement()));
    


    // RepeatCommand setAngler = new RepeatCommand(new moveAngler(shooter, 5));
    RepeatCommand setAngler2 = new RepeatCommand(new InstantCommand(() -> shooter.setAnglerNew()));

    j.oRight.whileTrue(new shoot(shooter, intake, angler, vision));
    // j.oRight.whileTrue(new shoot(shooter, intake, angler, vision));
    // j.oRight.whileTrue(new InstantCommand(() -> shooter.setAnglerNew()));
    j.oRight.whileFalse(new InstantCommand(() -> shooter.shootStop()));
    j.oRight.whileFalse(new InstantCommand(() -> intake.stopIndexer()));
    j.oRight.whileFalse(new InstantCommand(() -> angler.stopAngler()));

    j.oB.whileTrue(new InstantCommand(() -> climber.up()));
    j.oX.whileTrue(new InstantCommand(() -> climber.down()));
    j.oB.whileFalse(new InstantCommand(() -> climber.stop()));
    j.oX.whileFalse(new InstantCommand(() -> climber.stop()));

  }

  public RobotContainer() {
    configureBindings();
    dashboardStuff();
    runCurrentLimits();
    // autos.registerCommands();
    // drivetrain.configurePathPlanner();
    // NamedCommands.registerCommand("Intake", new intake(intake));
    // NamedCommands.registerCommand("Shoot", new shoot(shooter, intake, angler, vision));
    // autos.getAutos();


    SmartDashboard.putData("Field", Constants.Swerve.m_field);

  }

  public void dashboardStuff(){
    Pigeon2 gyro = new Pigeon2(50);
    ShuffleboardTab tab = Shuffleboard.getTab("Robot");
    tab.addNumber("Pitch: ", () -> gyro.getPitch().getValueAsDouble() );
    tab.addNumber("Yaw: ", () -> gyro.getYaw().getValueAsDouble());
    tab.addNumber("Roll: ", () -> gyro.getRoll().getValueAsDouble());
    tab.addNumber("Necessary Angle", () -> Math.toDegrees(Math.atan(66/(Vision.z * 39.37)))/5.14);
    tab.addNumber("Intake Torque Current", () -> intake.getTorqueCurrent());
    // tab.addNumber("Angler angle", () -> shooter.getAngle());

    gyro.close();
  }

  public void runCurrentLimits(){
    int[] swerveMotors = {11,12,21,22,31,32,41,42};
    int[] otherMotors = {60,61};

    TalonFXConfiguration config = new TalonFXConfiguration();

    CurrentLimitsConfigs swerveConfig = new CurrentLimitsConfigs();
    swerveConfig.SupplyCurrentLimitEnable = true;
    swerveConfig.SupplyCurrentLimit = 35;
    swerveConfig.SupplyCurrentThreshold = 60;
    swerveConfig.SupplyTimeThreshold = 0.1;

    
    CurrentLimitsConfigs currentConfig = new CurrentLimitsConfigs();
    currentConfig.StatorCurrentLimitEnable = true;
    currentConfig.StatorCurrentLimit = 50;

    currentConfig.SupplyCurrentLimitEnable = true;
    currentConfig.SupplyCurrentLimit = 35;
    currentConfig.SupplyCurrentThreshold = 60;
    currentConfig.SupplyTimeThreshold = 0.1;

    for(int i: swerveMotors){
      TalonFX motor = new TalonFX(i);
      motor.getConfigurator().apply(swerveConfig);
      motor.close();
    }

    for(int i: otherMotors){
      TalonFX motor = new TalonFX(i);
      motor.getConfigurator().apply(swerveConfig); /// !!!!! CURRENTLY WE ARE NOT DOING STATOR LIMITING ONLY SUPPLY LIMITING !!!!!
      motor.close();
    }

  }


  public Command getAutonomousCommand() {


    autos.registerCommands();
    List<Translation2d> bezierPoints = PathPlannerPath.bezierFromPoses(
        new Pose2d(0.0, 0.0, Rotation2d.fromDegrees(0)),
        new Pose2d(20.0, 20.0, Rotation2d.fromDegrees(0))

    );

    PathPlannerPath path = new PathPlannerPath(
            bezierPoints,
            new PathConstraints(5.0, 5.0, 2 * Math.PI, 4 * Math.PI), // The constraints for this path. If using a differential drivetrain, the angular constraints have no effect.
            new GoalEndState(3.0, Rotation2d.fromDegrees(-90)) // Goal end state. You can set a holonomic rotation here. If using a differential drivetrain, the rotation will have no effect.
    );


    /* First put the drivetrain into auto run mode, then run the auto */
    // return drivetrain.followPathCommand(path);

    // Command goForward = drivetrain.getAutoPath("Forward");
    // return goForward;
    return autos.sendAutos();
  }

  public void periodic(){
    // Do this in either robot or subsystem init
    // SmartDashboard.putData("Field", m_field);
    NamedCommands.registerCommand("Intake", new intake(intake));
    NamedCommands.registerCommand("Shoot", new shoot(shooter, intake, angler, vision));

    // Do this in either robot periodic or subsystem periodic ---- odometry
    // m_field.setRobotPose(drivetrain.getOdometry().getPoseMeters());
    // m_field.setRobotPose(Vision.estimator.getEstimatedPosition().getX(), Vision.estimator.getEstimatedPosition().getY(), Vision.estimator.getEstimatedPosition().getRotation());


  }
}