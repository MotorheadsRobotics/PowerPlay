package org.firstinspires.ftc.teamcode.Auton;

public class turnTest extends AdiRunner{
    @Override
    public void runOpMode() {
        robot.init(false);
        robot.initGyro();
        runtime.reset();
        turnToIMU(90, 0.5, robot);
    }
}