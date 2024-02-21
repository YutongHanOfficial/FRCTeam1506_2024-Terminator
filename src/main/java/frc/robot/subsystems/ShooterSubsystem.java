package frc.robot.subsystems;

import com.ctre.phoenix6.configs.MotorOutputConfigs;
import com.ctre.phoenix6.controls.VelocityDutyCycle;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.InvertedValue;
import com.ctre.phoenix6.signals.NeutralModeValue;

import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants;

public class ShooterSubsystem extends SubsystemBase {

  private TalonFX motor1 = new TalonFX(Constants.ShooterSubsystem.TopID);
  private TalonFX motor2 = new TalonFX(Constants.ShooterSubsystem.BottomID);

  double speed = 0.3;

  public double increment = 0.6;
      
  
  public ShooterSubsystem() {
    MotorOutputConfigs output = new MotorOutputConfigs();
    output.Inverted = InvertedValue.Clockwise_Positive;
    output.NeutralMode = NeutralModeValue.Brake;

    motor1.getConfigurator().apply(output);
    motor2.getConfigurator().apply(output);

    motor1.setInverted(false);
  }

  public void shootMax(){
    motor1.set(1);
    motor2.set(1);
    System.out.println("Shoot Max");
  }

  public void shootRPM(){
    VelocityDutyCycle request = new VelocityDutyCycle(3000);
    motor1.setControl(request);
    motor2.setControl(request);

  }

  public void shootStop(){
    motor1.set(0);
    motor2.set(0);
  }

  public double getIncrement(){
    return increment;
  }

  public void increaseIncrement(){
    increment+=0.1;
  }
  public void decreaseIncrement(){
    increment-=0.1;
  }


  public double getAvgShooterSpeed(){
    return (motor1.getVelocity().getValueAsDouble() + motor2.getVelocity().getValueAsDouble())/2;
  }


  @Override
  public void periodic() {
    // This method will be called once per scheduler run
  }
}