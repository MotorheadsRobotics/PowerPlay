/* Copyright (c) 2022 FIRST. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted (subject to the limitations in the disclaimer below) provided that
 * the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list
 * of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * Neither the name of FIRST nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
 * LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.firstinspires.ftc.teamcode.Teleop;

import com.acmerobotics.roadrunner.drive.MecanumDrive;
import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.acmerobotics.roadrunner.geometry.Vector2d;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.Auton.AutonDriving;
import org.firstinspires.ftc.teamcode.Hardware.Hardware;
import org.firstinspires.ftc.teamcode.Roadrunner.*;
import org.firstinspires.ftc.teamcode.Roadrunner.drive.SampleMecanumDrive;

/**
 *  In OnBot Java, add a new OpMode, drawing from this Sample; select TeleOp.
 *  Also add another new file named RobotHardware.java, drawing from the Sample with that name; select Not an OpMode.
 */

@TeleOp(name="Duo Field Centric", group="Robot")
public class DuoFieldCentric extends AutonDriving {
    Hardware robot = new Hardware(this);
//  goal heights (in) {0, 2, 15, 23, 32}

    public int currentPreset = 0;
    private static final double LIFTMOTORPOWER = 0.8;
    public ElapsedTime runtime = new ElapsedTime();
    public static double FLIPDELAY = 1100; // milliseconds

    @Override
    public void runOpMode() {
        double clawPosition = 1;
        double flipPosition = 0;
        double time = 0;
        double rotatePosition = 1;
        boolean isRotated = true;
        boolean isFlipped = true;
        double speedMultiplier = 1;
        boolean lock = false;

        // initialize all the hardware, using the hardware class. See how clean and simple this is?
        robot.init(true);
        robot.initGyro();

        SampleMecanumDrive wheels = new SampleMecanumDrive(hardwareMap);
        wheels.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        wheels.setPoseEstimate(new Pose2d(0,0,0));

        // Send telemetry message to signify robot waiting;
        // Wait for the game to start (driver presses PLAY)
        waitForStart();

        // boolean slow = false;
        robot.upMotorL.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        robot.upMotorR.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        robot.upMotorL.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        robot.upMotorR.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        int offsetCounts = 0;
        // run until the end of the match (driver presses STOP)
        while (opModeIsActive()) {
            // lock moving controls
            if(gamepad2.left_stick_button && gamepad2.right_stick_button){
                lock = !lock;
            }

            // Drive robot via mecanum
            // Slow mode mapped to left bumper
            speedMultiplier = gamepad1.right_trigger > 0.3 ? 0.5 : 1;
            if(lock)
                speedMultiplier = 0;

            Pose2d poseEstimate = wheels.getPoseEstimate();

            // Create a vector from the gamepad x/y inputs
            // Then, rotate that vector by the inverse of that heading
            Vector2d input = new Vector2d(
                    -gamepad1.left_stick_y * speedMultiplier,
                    -gamepad1.left_stick_x * speedMultiplier
            ).rotated(-poseEstimate.getHeading());

            // Pass in the rotated input + right stick value for rotation
            // Rotation is not part of the rotated input thus must be passed in separately
            wheels.setWeightedDrivePower(
                    new Pose2d(
                            input.getX(),
                            input.getY(),
                            -gamepad1.right_stick_x * speedMultiplier
                    )
            );

            // Update everything. Odometry. Etc.
            wheels.update();

            // Speed Multiplier Telemetry
            telemetry.addData("Speed Multiplier: ", speedMultiplier);

            // Claw mapped to a
            if (gamepad2.a && robot.upMotorL.getCurrentPosition() + robot.upMotorR.getCurrentPosition() > Hardware.minHeightForFlip * 2) {
                robot.downDropUp(Hardware.heightsCounts[currentPreset]);
                while(gamepad2.a) {}
            }
            else if(gamepad2.a) {
                clawPosition = 1 - clawPosition;
                while(gamepad2.a) {}
            }
            robot.claw.setPosition(clawPosition);

            // Flipping mapped to y
            if (gamepad2.y) { // flipPosition = 0 means default state
                flipPosition = 1 - flipPosition;
                while(gamepad2.y) {}
                isRotated = false;
                isFlipped = false;
            }

            if(isFlipped){
                robot.flipL.setPosition(Hardware.FLIP_CONSTANT * (1 - flipPosition));
                robot.flipR.setPosition(Hardware.FLIP_CONSTANT * flipPosition);
            }
            else if(robot.upMotorL.getCurrentPosition() + robot.upMotorR.getCurrentPosition() <= 2 * Hardware.minHeightForFlip){
                robot.flipL.setPosition(Hardware.FLIP_CONSTANT * flipPosition);
                robot.flipR.setPosition(Hardware.FLIP_CONSTANT * (1 - flipPosition));
            }
            else if(robot.upMotorL.getCurrentPosition() + robot.upMotorR.getCurrentPosition() > 2 * Hardware.minHeightForFlip){
                robot.claw.setPosition(1);
                robot.flipL.setPosition(Hardware.FLIP_CONSTANT * (1 - flipPosition));
                robot.flipR.setPosition(Hardware.FLIP_CONSTANT * flipPosition);
                time = runtime.milliseconds();
                isFlipped = true;
            }

            //auto rotate after flip
            if(!isRotated && Math.abs(runtime.milliseconds() - time - FLIPDELAY) <= 175) {
                rotatePosition = 1 - rotatePosition;
                isRotated = true;
            }

            if(gamepad2.x){
                rotatePosition = 1 - rotatePosition;
                while(gamepad2.x){}
            }
            robot.rotate.setPosition(Hardware.ROTATE_CONSTANT * rotatePosition);

            // Lifts
            telemetry.addData("leftLiftPos", robot.upMotorL.getCurrentPosition());
            telemetry.addData("rightLiftPos", robot.upMotorR.getCurrentPosition());

            // Offset Calculations using bumpers
            if(gamepad2.left_trigger > 0.3){
                offsetCounts -= 42;
            }
            else if(gamepad2.right_trigger > 0.3){
                offsetCounts += 42;
            }

            // dpad setting presets
            if(gamepad2.dpad_up){
                offsetCounts = 0;
                currentPreset = 1;
                while(gamepad2.dpad_up) {}
            }
            else if(gamepad2.dpad_down){
                offsetCounts = 0;
                currentPreset = 0;
                while(gamepad2.dpad_down) {}
            }
            else if(gamepad2.dpad_right){ // right is medium
                offsetCounts = 0;
                currentPreset = 3;
                while(gamepad2.dpad_right) {}
            }
            else if(gamepad2.dpad_left){ // left is low
                offsetCounts = 0;
                currentPreset = 2;
                while(gamepad2.dpad_left) {}
            }

            // Move Lifts
            telemetry.addData("Preset Mode", true);
            telemetry.addData("Current Preset", Hardware.heightNames[currentPreset]);

            telemetry.addData("Heading", robot.getRawHeading());
            robot.setLift(Hardware.heightsCounts[currentPreset] + offsetCounts, LIFTMOTORPOWER);

            telemetry.update();

            // Pace this loop so hands move at a reasonable speed.
            sleep(25);
        }
    }
}