/* Copyright (c) 2017 FIRST. All rights reserved.
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

package org.firstinspires.ftc.teamcode.Auton;

import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;
import org.firstinspires.ftc.teamcode.Hardware.Hardware;
import org.opencv.core.Mat;
import org.opencv.objdetect.QRCodeDetector;
import org.openftc.easyopencv.OpenCvPipeline;
import org.openftc.easyopencv.OpenCvCamera;
import org.openftc.easyopencv.OpenCvCameraRotation;

@TeleOp(name="Basic: Iterative OpMode", group="Iterative Opmode")
@Disabled
public class IterativeAuton extends OpMode
{
    Hardware robot = new Hardware(this);
    ElapsedTime runtime = new ElapsedTime();
    private String finalMessage = "";
    private boolean hasStarted = false;
    private boolean isStopped = false;

    static final double     COUNTS_PER_MOTOR_REV    = 384.5 ;       // from GoBuilda
    static final double     DRIVE_GEAR_REDUCTION    = 1.0 ;         // Gearing up (more speed, less torque) --> ratio < 1.0
    static final double     WHEEL_DIAMETER_INCHES   = 3.77952756 ;  // 96mm
    static final double     COUNTS_PER_INCH         = (COUNTS_PER_MOTOR_REV * DRIVE_GEAR_REDUCTION) /
            (WHEEL_DIAMETER_INCHES * Math.PI);
    static final double     DRIVE_SPEED             = 0.6;
    static final double     TURN_SPEED              = 0.5;

    static final double     P_TURN_GAIN            = 0.02;     // Larger is more responsive, but also less stable
    static final double     P_DRIVE_GAIN           = 0.03;     // Larger is more responsive, but also less stable
    static final double     HEADING_THRESHOLD      = 1;

    private double          robotHeading  = 0;
    private double          headingOffset = 0;
    private double          headingError  = 0;

    private double  targetHeading = 0;
    private double  driveSpeed    = 0;
    private double  turnSpeed     = 0;
    private double  leftSpeed     = 0;
    private double  rightSpeed    = 0;
    private int     leftTarget    = 0;
    private int     rightTarget   = 0;

    /*
     * Code to run ONCE when the driver hits INIT
     */
    @Override
    public void init() {
        robot.init();
        robot.initQRCodeSensor();
    }

    /*
     * Code to run REPEATEDLY after the driver hits INIT, but before they hit PLAY
     */
    @Override
    public void init_loop() {
        telemetry.addData("Message: ", robot.message);
        telemetry.update();
    }

    /*
     * Code to run ONCE when the driver hits PLAY
     */
    @Override
    public void start() {
        hasStarted = true;
        runtime.reset();
        telemetry.addData("Message: ", robot.message);
        telemetry.update();
        sleep(1000);
        finalMessage = robot.message;

        encoderDrive(1,0,29, 2);
        try {
            switch (finalMessage) {
                case "https://left.com":
                    // strafe left one tile
                    encoderDrive(1, 270, 24, 1.5);
                    break;
                case "https://middle.com":
                    // no need to move
                    break;
                case "https://right.com":
                    // strafe right one tile
                    encoderDrive(1, 90, 24, 1.5);
                    break;
                default:
                    // hope it's middle, attempting to recheck
                    throw new NullPointerException();
            }
        } catch(NullPointerException e){
            telemetry.addData("Message: ", "QR Code not found. Trying again. ");
            finalMessage = robot.message;
            try {
                switch (finalMessage) {
                    case "https://left.com":
                        // strafe left one tile
                        encoderDrive(1, 270, 24, 1.5);
                        break;
                    case "https://middle.com":
                        // no need to move
                        break;
                    case "https://right.com":
                        // strafe right one tile
                        encoderDrive(1, 90, 24, 1.5);
                        break;
                    default:
                        // hope it's middle, attempting to recheck
                        throw new NullPointerException();
                }
            }catch(NullPointerException exception){
                telemetry.addData("Message: ", "QR Code still not found. Giving up. ");
            }
        }
        // path done
    }

    /*
     * Code to run REPEATEDLY after the driver hits PLAY but before they hit STOP
     */
    @Override
    public void loop() {
    }

    /*
     * Code to run ONCE after the driver hits STOP
     */
    @Override
    public void stop() {
        isStopped = true;
    }

    /**
     * Method to drive at any given angle for a certain number of degrees. Maintains heading.
     * @param speed desired magnitude speed for the robot
     * @param direction forwards = 0, right = 90, backwards = 180, left = 270
     * @param inches you know what this means
     * @param timeoutS how many seconds until the robot gives up on this task
     */
    public void encoderDrive(double speed, double direction, double inches, double timeoutS) {
        int newFrontLeftTarget;
        int newFrontRightTarget;
        int newBackLeftTarget;
        int newBackRightTarget;
        robot.stopAndResetDriveEncoders();
        // Send telemetry message to indicate successful Encoder reset
        telemetry.addData("Starting at",  "%7d :%7d :%7d :%7d",
                robot.fLMotor.getCurrentPosition(),
                robot.fRMotor.getCurrentPosition(),
                robot.bLMotor.getCurrentPosition(),
                robot.bRMotor.getCurrentPosition());
        telemetry.update();

        // Ensure that the opmode is still active
        if (opModeIsActive()) {
            // Determine new target position, and pass to motor controller

            /** Direction: forwards = 0, right = 90, back = 180, left = 270 **/
            // direction -= 90; direction *= -1; direction *= Math.PI/180;
            direction = (90 - direction) * Math.PI / 180 - Math.PI / 4;
            double v1 = inches * Math.cos(direction);
            double v2 = inches * Math.sin(direction);
            double v3 = inches * Math.sin(direction);
            double v4 = inches * Math.cos(direction);

            newFrontLeftTarget = robot.fLMotor.getCurrentPosition() + (int)(v1 * COUNTS_PER_INCH);
            newFrontRightTarget = robot.fRMotor.getCurrentPosition() + (int)(v2 * COUNTS_PER_INCH);
            newBackLeftTarget = robot.bLMotor.getCurrentPosition() + (int)(v3 * COUNTS_PER_INCH);
            newBackRightTarget = robot.bRMotor.getCurrentPosition() + (int)(v4 * COUNTS_PER_INCH);

            robot.fLMotor.setTargetPosition(newFrontLeftTarget);
            robot.fRMotor.setTargetPosition(newFrontRightTarget);
            robot.bLMotor.setTargetPosition(newBackLeftTarget);
            robot.bRMotor.setTargetPosition(newBackRightTarget);

            // Turn On RUN_TO_POSITION
            robot.fLMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            robot.fRMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            robot.bLMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            robot.bRMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);

            // reset the timeout time and start motion.
            runtime.reset();
            double forwardSlashSpeed = Math.abs(speed) * Math.cos(direction);
            double backwardsSlashSpeed = Math.abs(speed) * Math.sin(direction);
            robot.fLMotor.setPower(forwardSlashSpeed);
            robot.fRMotor.setPower(backwardsSlashSpeed);
            robot.bLMotor.setPower(backwardsSlashSpeed);
            robot.bRMotor.setPower(forwardSlashSpeed);

            // keep looping while we are still active, and there is time left, and both motors are running.
            // Note: We use (isBusy() && isBusy()) in the loop test, which means that when EITHER motor hits
            // its target position, the motion will stop.  This is "safer" in the event that the robot will
            // always end the motion as soon as possible.
            // However, if you require that BOTH motors have finished their moves before the robot continues
            // onto the next step, use (isBusy() || isBusy()) in the loop test.
            while (opModeIsActive() &&
                    (runtime.seconds() < timeoutS) &&
                    (robot.fLMotor.isBusy() || robot.fRMotor.isBusy() || robot.bLMotor.isBusy() || robot.bRMotor.isBusy())) {

                // Display it for the driver.
                telemetry.addData("Running to",  " %7d :%7d :%7d :%7d", newFrontLeftTarget,  newFrontRightTarget, newBackLeftTarget, newBackRightTarget);
                telemetry.addData("Currently at",  " at %7d :%7d :%7d :%7d",
                        robot.fLMotor.getCurrentPosition(), robot.fRMotor.getCurrentPosition(), robot.bLMotor.getCurrentPosition(), robot.bRMotor.getCurrentPosition());
                telemetry.update();
            }

            // Stop all motion;
            robot.fLMotor.setPower(0);
            robot.fRMotor.setPower(0);
            robot.bLMotor.setPower(0);
            robot.bRMotor.setPower(0);

            // Turn off RUN_TO_POSITION
            robot.fLMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            robot.fRMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            robot.fLMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            robot.fRMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            sleep(250);   // optional pause after each move.
        }
    }

    /**
     *
     * @param speed desired speed at which wheels turn
     * @param leftInches how many inches the left-side wheels turn
     * @param rightInches same for right-side wheels
     * @param timeoutS seconds until robot gives up on life
     */
    public void encoderDriveSimple(double speed,
                                   double leftInches, double rightInches,
                                   double timeoutS) {
        int newfLeftTarget;
        int newfRightTarget;
        int newbLeftTarget;
        int newbRightTarget;

        // Ensure that the opmode is still active
        if (opModeIsActive()) {

            // Determine new target position, and pass to motor controller
            newfLeftTarget = robot.fLMotor.getCurrentPosition() + (int)(leftInches * COUNTS_PER_INCH);
            newfRightTarget = robot.fRMotor.getCurrentPosition() + (int)(rightInches * COUNTS_PER_INCH);
            newbLeftTarget = robot.bLMotor.getCurrentPosition() + (int)(leftInches * COUNTS_PER_INCH);
            newbRightTarget = robot.bRMotor.getCurrentPosition() + (int)(rightInches * COUNTS_PER_INCH);
            robot.fLMotor.setTargetPosition(newfLeftTarget);
            robot.fRMotor.setTargetPosition(newfRightTarget);
            robot.bLMotor.setTargetPosition(newbLeftTarget);
            robot.bRMotor.setTargetPosition(newbRightTarget);

            // Turn On RUN_TO_POSITION
            robot.fRMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            robot.fLMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            robot.bRMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            robot.bLMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);

            // reset the timeout time and start motion.
            runtime.reset();
            robot.fRMotor.setPower(Math.abs(speed));
            robot.fLMotor.setPower(Math.abs(speed));
            robot.bRMotor.setPower(Math.abs(speed));
            robot.bLMotor.setPower(Math.abs(speed));

            // keep looping while we are still active, and there is time left, and both motors are running.
            // Note: We use (isBusy() && isBusy()) in the loop test, which means that when EITHER motor hits
            // its target position, the motion will stop.  This is "safer" in the event that the robot will
            // always end the motion as soon as possible.
            // However, if you require that BOTH motors have finished their moves before the robot continues
            // onto the next step, use (isBusy() || isBusy()) in the loop test.
            while (opModeIsActive() &&
                    (runtime.seconds() < timeoutS) &&
                    (robot.fRMotor.isBusy() && robot.bLMotor.isBusy() && robot.fLMotor.isBusy() && robot.bRMotor.isBusy())) {

                // Display it for the driver.
                telemetry.addData("Running to",  " %7d :%7d :%7d :%7d", newfLeftTarget,  newfRightTarget, newbLeftTarget, newbRightTarget);
                telemetry.addData("Currently at",  " at %7d :%7d :%7d :%7d",
                        robot.fLMotor.getCurrentPosition(), robot.fRMotor.getCurrentPosition(), robot.bLMotor.getCurrentPosition(), robot.bRMotor.getCurrentPosition());
                telemetry.update();
            }

            // Stop all motion;
            robot.fLMotor.setPower(0);
            robot.fRMotor.setPower(0);
            robot.bLMotor.setPower(0);
            robot.bRMotor.setPower(0);

            // Turn off RUN_TO_POSITION
            robot.fRMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            robot.fLMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            robot.bRMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            robot.bLMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

            sleep(250);   // optional pause after each move.
        }
    }

    /**
     *  Method to spin on central axis to point in a new direction.
     *  Move will stop if either of these conditions occur:
     *  1) Move gets to the heading (angle)
     *  2) Driver stops the opmode running.
     *
     * @param maxTurnSpeed Desired MAX speed of turn. (range 0 to +1.0)
     * @param heading Absolute Heading Angle (in Degrees) relative to last gyro reset.
     *              0 = fwd. +ve is CCW from fwd. -ve is CW from forward.
     *              If a relative angle is required, add/subtract from current heading.
     */
    public void turnToHeading(double maxTurnSpeed, double heading) {

        // Run getSteeringCorrection() once to pre-calculate the current error
        getSteeringCorrection(heading, P_DRIVE_GAIN);

        // keep looping while we are still active, and not on heading.
        while (opModeIsActive() && (Math.abs(headingError) > HEADING_THRESHOLD)) {

            // Determine required steering to keep on heading
            turnSpeed = getSteeringCorrection(heading, P_TURN_GAIN);

            // Clip the speed to the maximum permitted value.
            turnSpeed = Range.clip(turnSpeed, -maxTurnSpeed, maxTurnSpeed);

            // Pivot in place by applying the turning correction
            turnRobot(turnSpeed);

            // Display drive status for the driver.
            sendTelemetry();
        }

        // Stop all motion;
        stopAllMotion();
    }

    /**
     *  Method to obtain & hold a heading for a finite amount of time
     *  Move will stop once the requested time has elapsed
     *  This function is useful for giving the robot a moment to stabilize it's heading between movements.
     *
     * @param maxTurnSpeed      Maximum differential turn speed (range 0 to +1.0)
     * @param heading    Absolute Heading Angle (in Degrees) relative to last gyro reset.
     *                   0 = fwd. +ve is CCW from fwd. -ve is CW from forward.
     *                   If a relative angle is required, add/subtract from current heading.
     * @param holdTime   Length of time (in seconds) to hold the specified heading.
     */
    public void holdHeading(double maxTurnSpeed, double heading, double holdTime) {

        ElapsedTime holdTimer = new ElapsedTime();
        holdTimer.reset();

        // keep looping while we have time remaining.
        while (opModeIsActive() && (holdTimer.time() < holdTime)) {
            // Determine required steering to keep on heading
            turnSpeed = getSteeringCorrection(heading, P_TURN_GAIN);

            // Clip the speed to the maximum permitted value.
            turnSpeed = Range.clip(turnSpeed, -maxTurnSpeed, maxTurnSpeed);

            // Pivot in place by applying the turning correction
            turnRobot(turnSpeed);

            // Display drive status for the driver.
            sendTelemetry();
        }

        // Stop all motion;
        stopAllMotion();
    }

    // **********  LOW Level driving functions.  ********************

    /**
     * This method uses a Proportional Controller to determine how much steering correction is required.
     *
     * @param desiredHeading        The desired absolute heading (relative to last heading reset)
     * @param proportionalGain      Gain factor applied to heading error to obtain turning power.
     * @return                      Turning power needed to get to required heading.
     */
    public double getSteeringCorrection(double desiredHeading, double proportionalGain) {
        targetHeading = desiredHeading;  // Save for telemetry

        // Get the robot heading by applying an offset to the IMU heading
        robotHeading = getRawHeading() - headingOffset;

        // Determine the heading current error
        headingError = targetHeading - robotHeading;

        // Normalize the error to be within +/- 180 degrees
        while (headingError > 180)  headingError -= 360;
        while (headingError <= -180) headingError += 360;

        // Multiply the error by the gain to determine the required steering correction/  Limit the result to +/- 1.0
        return Range.clip(headingError * proportionalGain, -1, 1);
    }

    /**
     * This method takes separate drive (fwd/rev) and turn (right/left) requests,
     * combines them, and applies the appropriate speed commands to the left and right wheel motors.
     * @param turn  clockwise turning motor speed.
     */
    public void turnRobot(double turn) {
        turnSpeed  = turn;      // save this value as a class member so it can be used by telemetry.

        leftSpeed  = -turn;
        rightSpeed = turn;

        // Scale speeds down if either one exceeds +/- 1.0;
        double max = Math.max(Math.abs(leftSpeed), Math.abs(rightSpeed));
        if (max > 1.0)
        {
            leftSpeed /= max;
            rightSpeed /= max;
        }

        robot.fLMotor.setPower(leftSpeed);
        robot.bLMotor.setPower(leftSpeed);
        robot.fRMotor.setPower(rightSpeed);
        robot.bRMotor.setPower(rightSpeed);
    }

    public void stopAllMotion(){
        robot.fLMotor.setPower(0);
        robot.fRMotor.setPower(0);
        robot.bLMotor.setPower(0);
        robot.bRMotor.setPower(0);
    }

    /**
     *  Display the various control parameters while driving
     */
    private void sendTelemetry() {
        telemetry.addData("Motion", "Turning");

        telemetry.addData("Angle Target:Current", "%5.2f:%5.0f", targetHeading, robotHeading);
        telemetry.addData("Error:Steer",  "%5.1f:%5.1f", headingError, turnSpeed);
        telemetry.addData("Wheel Speeds L:R.", "%5.2f : %5.2f", leftSpeed, rightSpeed);
        telemetry.update();
    }

    /**
     * read the raw (un-offset Gyro heading) directly from the IMU
     */
    public double getRawHeading() {
        Orientation angles   = robot.imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES);
        return angles.firstAngle;
    }

    public final void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    public final boolean opModeIsActive() {
        boolean isActive = !isStopped && hasStarted;
        if (isActive) {
            idle();
        }
        return isActive;
    }
    public final void idle() {
        // Otherwise, yield back our thread scheduling quantum and give other threads at
        // our priority level a chance to run
        Thread.yield();
    }
    public class SimplePipeline extends OpenCvPipeline {
        public Mat processFrame(Mat input){
            String decoded = robot.det.detectAndDecode(input);
            if (decoded.length() > 5){
                robot.found = true;
                robot.message = decoded;
            }
            return input;
        }
    }
}