package org.firstinspires.ftc.teamcode.AutonRunner;

import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.acmerobotics.roadrunner.geometry.Vector2d;
import com.acmerobotics.roadrunner.trajectory.Trajectory;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

import org.firstinspires.ftc.teamcode.Hardware.Camera;
import org.firstinspires.ftc.teamcode.Hardware.Lift;
import org.firstinspires.ftc.teamcode.Roadrunner.drive.SampleMecanumDrive;
import org.openftc.apriltag.AprilTagDetection;

@Autonomous(name="Asynchronous", group="Robot")
public class CalibratorLeftStackRunnerAdityaAndKevinTestVersionThatMayOrMayNotWorkButDibyadeepSaidWeCouldDoItInThirtyMinutes extends AutonomousDriving {
    AprilTagDetection tagOfInterest = null;
    //TODO: change lift presets to what they actually are.
    double adjustment = 0.0;

    @Override
    public void runOpMode() throws InterruptedException {
        SampleMecanumDrive robot = new SampleMecanumDrive(hardwareMap);
        Lift lift = new Lift(this, true);
        Camera tagDetector = new Camera(this);
        lift.flipToPosition(0.5);
        tagOfInterest = getTag(tagDetector.initAprilTagDetection());

        robot.setPoseEstimate(new Pose2d(-36,-65.25,0));

        track1 = robot.trajectoryBuilder(robot.getPoseEstimate(), true)
                .addDisplacementMarker(() -> lift.setLift(Lift.highInch * Lift.liftCountsPerInch, Lift.LIFTMOTORPOWER))
                .addTemporalMarker(0.5, () -> lift.flipToPosition(1))
                .addTemporalMarker(1.5, () -> lift.setRotate(1))
                .strafeLeft(47.25)
                .splineToSplineHeading(new Pose2d(-30.8,-6.8,Math.toRadians(225)), Math.toRadians(45))
                //TODO: make robot not run into pole
                .lineTo(new Vector2d(-27.6,-3.6))
                .build();


         trackCreator trackMod = new trackCreator() {
             @Override
             public void track2Mod(double cone) {
                 // it's possible we may need to hardcode a start point instead of getting the current estimate.
                 // don't know which would cause less drift
                 robot.setPoseEstimate(robot.getPoseEstimate().plus(new Pose2d(0,-adjustment)));
                 track2 = robot.trajectoryBuilder(robot.getPoseEstimate())
                         .addDisplacementMarker(() -> lift.setLift((int)cone, Lift.LIFTMOTORPOWER))
                         .addTemporalMarker(0.15, () -> lift.flipToPosition(0))
                         .addTemporalMarker(0.3, () -> {
                             lift.setRotate(0);
                             lift.openClaw();
                         })
                         .addTemporalMarker(0.1, () -> lift.closeClaw())
                         //TODO: Make robot not run into wall
                         .splineTo(new Vector2d(-61.0, -12), Math.toRadians(180)) // theoretically this point should be (-63.5, -12) but variations idk
                         .build();
             }

             @Override
             public void track3Update(int offset) {
                 track3 = robot.trajectoryBuilder(robot.getPoseEstimate(), true)
                         .addTemporalMarker(0.5, () -> lift.flipToPosition(1))
                         .addTemporalMarker(1.5, () -> lift.setRotate(1))
                         .addDisplacementMarker(() -> lift.setLift(Lift.highInch * Lift.liftCountsPerInch + offset, Lift.LIFTMOTORPOWER))
                         //TODO: copy from track 1 to not have it run into pole
                         .splineTo(new Vector2d(-27.6, -3.6), Math.toRadians(45))
                         .build();
             }
         };

        waitForStart();

        if(isStopRequested()) return;

        robot.followTrajectory(track1, this);
        telemetry.addData("Path: ", "Track 1 Completed - Preloaded Cone");
        telemetry.update();
        lift.downDrop();

        for (int i = 4; i >= 1; i--) {
            // Go towards cone stack
            trackMod.track2Mod(cones[i]);
            robot.followTrajectory(track2,this);
            lift.closeClaw();
            sleep(100);
            telemetry.addData("Path: ", "Track 2 Completed - (" + (5 - i) + "/5)");
            telemetry.update();

            // Go back to high goal
            trackMod.track3Update((4 - i) * 166);
            robot.followTrajectory(track3, this);
            lift.downDrop();
            telemetry.addData("Path: ", "Track 3 Completed - (" + (5 - i) + "/5)");
            telemetry.update();
        }

        // Park in designated spot
        Trajectory track4 = null;
        int loopIterations = 4;
        robot.setPoseEstimate(robot.getPoseEstimate().plus(new Pose2d(0,adjustment * loopIterations)));
        switch (tagOfInterest.id) {
            case 1: // Park Left
                track4 = robot.trajectoryBuilder(robot.getPoseEstimate(), false)
                        .splineTo(new Vector2d(-36,-28),Math.toRadians(270))
                        .splineToConstantHeading(new Vector2d(-60,-36),Math.toRadians(180))
                        .build();
                break;
            case 3: // Park Right
                track4 = robot.trajectoryBuilder(robot.getPoseEstimate(), false)
                        .splineTo(new Vector2d(-36,-28),Math.toRadians(270))
                        .splineToConstantHeading(new Vector2d(-12,-36),Math.toRadians(0))
                        .build();
                break;
            default: // Park Middle / or guess middle if no tag found
                track4 = robot.trajectoryBuilder(robot.getPoseEstimate(), false)
                        .splineTo(new Vector2d(-36,-36),Math.toRadians(270))
                        .build();
        }
        robot.followTrajectory(track4, this);
        telemetry.addData("Path: ", "Track 4 Completed - Park");
        telemetry.update();

        sleep(10000);
    }
}
