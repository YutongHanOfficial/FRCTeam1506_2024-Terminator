// Copyright (c) FIRST and other WPILib contributors. Open Source Software.
// FRC TEAM 1506 --- METAL MUSCLE --- 2024 ROBOT CODE --- TERMINATOR

package frc.robot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.RepeatCommand;
import frc.robot.subsystems.Angler;
import frc.robot.subsystems.Climber;
import frc.robot.subsystems.ShooterSubsystem;
import frc.robot.subsystems.Vision;
import frc.robot.commands.intake.intake;
import frc.robot.commands.shooter.angle;
import frc.robot.commands.shooter.shoot;
import frc.robot.commands.shooter.shootAmp;
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
  public final frc.robot.subsystems.ShooterSubsystem shooter = new frc.robot.subsystems.ShooterSubsystem();
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

    // zero gyro
    j.dB.onTrue(drivetrain.runOnce(() -> drivetrain.seedFieldRelative()));

    if (Utils.isSimulation()) {
      drivetrain.seedFieldRelative(new Pose2d(new Translation2d(), Rotation2d.fromDegrees(90)));
    }
    // drivetrain.registerTelemetry(logger::telemeterize);

    // j.dUp.whileTrue(drivetrain.applyRequest(() -> forwardStraight.withVelocityX(0.5).withVelocityY(0)));
    // j.dDown.whileTrue(drivetrain.applyRequest(() -> forwardStraight.withVelocityX(-0.5).withVelocityY(0)));
    
    // j.dLeft.whileTrue(new RepeatCommand(new vision2(vision))); //shooter alignment
    j.dLeft.whileTrue(new vision2(vision).until(() -> vision.x > -Constants.Limelight.shooterThreshold && vision.x < Constants.Limelight.shooterThreshold)); //shooter alignment
    j.dRight.whileTrue(new kevin2(vision)); //intake alignment

    //ACTUAL CONTROLS!!!

    //intake
    j.oRT.whileTrue(new RepeatCommand(new InstantCommand(() -> intake.intake())));
    j.oRT.whileFalse(new InstantCommand(() -> intake.stopIntakeWithReset()));
    j.oLT.whileTrue(new InstantCommand(() -> intake.outtake()));
    j.oLT.whileFalse(new InstantCommand(() -> intake.stopIntake()));

    //delete
    j.dRT.whileTrue(new RepeatCommand(new InstantCommand(() -> intake.intake())));
    j.dRT.whileFalse(new InstantCommand(() -> intake.stopIntakeWithReset()));
    j.dLT.whileTrue(new InstantCommand(() -> intake.outtake()));
    j.dLT.whileFalse(new InstantCommand(() -> intake.stopIntake()));


    //manual shooter
    j.oRB.whileTrue(new InstantCommand(() -> shooter.shootMax()));
    j.oRB.whileFalse(new InstantCommand(() -> shooter.shootStop()));

    //manual angler
    j.oUp.whileTrue(new InstantCommand(() -> angler.anglerUp()));
    j.oDown.whileTrue(new InstantCommand(() -> angler.anglerDown()));
    j.oA.whileTrue(new InstantCommand(() -> angler.anglerZero()));
    j.oUp.whileFalse(new InstantCommand(() -> angler.stopAngler()));
    j.oDown.whileFalse(new InstantCommand(() -> angler.stopAngler()));
    
    //auto shooting
    j.oRight.whileTrue(new shoot(shooter, intake, angler, vision));
    j.oRight.whileFalse(new InstantCommand(() -> shooter.shootStop()));
    j.oRight.whileFalse(new InstantCommand(() -> intake.stopIndexer()));
    j.oRight.whileFalse(new InstantCommand(() -> angler.stopAngler()));

    j.dRight.whileTrue(new shoot(shooter, intake, angler, vision));
    j.dRight.whileFalse(new InstantCommand(() -> shooter.shootStop()));
    j.dRight.whileFalse(new InstantCommand(() -> intake.stopIndexer()));
    j.dRight.whileFalse(new InstantCommand(() -> angler.stopAngler()));

    j.dTouchpad.whileTrue(new shootAmp(angler, shooter, intake));
    j.dTouchpad.whileFalse(new InstantCommand(() -> shooter.shootStop()));
    j.dTouchpad.whileFalse(new InstantCommand(() -> intake.stopIndexer()));
    j.dTouchpad.whileFalse(new InstantCommand(() -> angler.stopAngler()));



    //climber
    j.oB.whileTrue(new InstantCommand(() -> climber.up()));
    j.oX.whileTrue(new InstantCommand(() -> climber.down()));
    j.oB.whileFalse(new InstantCommand(() -> climber.stop()));
    j.oX.whileFalse(new InstantCommand(() -> climber.stop()));

    //increment testing
    j.dShare.whileTrue(new InstantCommand(() -> shooter.decreaseIncrement()));
    j.dOptions.whileTrue(new InstantCommand(() -> shooter.increaseIncrement()));
    j.oTouchpad.whileTrue(new angle(angler, shooter.increment));
    j.oTouchpad.whileFalse(new InstantCommand(() -> angler.stopAngler()));
  }

  private final intake intakecommand = new intake(intake);

  public RobotContainer() {
    Map<String, Command> map = new HashMap<String, Command>();
    map.put("IntakeCommand", intakecommand);
    // map.put("Shoot", new shoot(shooter, intake, angler, vision));

    NamedCommands.registerCommands(map);

    configureBindings();
    dashboardStuff();
    runCurrentLimits();

    drivetrain.configurePathPlanner();

    autos.registerCommands();
    autos.getAutos();

    //our lovely field
    // SmartDashboard.putData("Field", Constants.Swerve.m_field);

  }

  public void dashboardStuff(){
    Pigeon2 gyro = new Pigeon2(50);
    ShuffleboardTab tab = Shuffleboard.getTab("Robot");
    tab.addNumber("Pitch: ", () -> gyro.getPitch().getValueAsDouble() );
    tab.addNumber("Yaw: ", () -> gyro.getYaw().getValueAsDouble());
    tab.addNumber("Roll: ", () -> gyro.getRoll().getValueAsDouble());
    // tab.addNumber("Necessary Angle", () -> Math.toDegrees(Math.atan(66/(Vision.z * 39.37)))/5.14);
    tab.addNumber("Intake Torque Current", () -> intake.getTorqueCurrent());
    tab.addNumber("Angler increment", () -> shooter.increment);

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

    // Do this in either robot periodic or subsystem periodic ---- odometry
    // m_field.setRobotPose(drivetrain.getOdometry().getPoseMeters());
    // m_field.setRobotPose(Vision.estimator.getEstimatedPosition().getX(), Vision.estimator.getEstimatedPosition().getY(), Vision.estimator.getEstimatedPosition().getRotation());


  }
}