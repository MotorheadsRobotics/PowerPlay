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

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.tfod.TFObjectDetector;
import org.firstinspires.ftc.teamcode.Hardware.Hardware;

@Autonomous(name="Encoder Test", group="Robot")

public class EncoderCountTesting extends LinearOpMode {
    // Initialize Hardware
    Hardware robot = new Hardware(this);
    ElapsedTime runtime = new ElapsedTime();

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

    @Override
    public void runOpMode() {
        robot.init();
        robot.initGyro();
        // Wait for the game to start (driver presses PLAY)
        waitForStart();

        // turnToHeading(90,0.05,0.2); // rotate left 90 degrees
        encoderDrive(1,0,10,3);

        telemetry.addData("Path", "Done");
        telemetry.update();
        sleep(50);
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
     * Initialize the Vuforia localization engine.
     */
    private void initVuforia() {
        /*
         * Configure Vuforia by creating a Parameter object, and passing it to the Vuforia engine.
         */
        VuforiaLocalizer.Parameters parameters = new VuforiaLocalizer.Parameters();

        parameters.vuforiaLicenseKey = VUFORIA_KEY;
        parameters.cameraName = hardwareMap.get(WebcamName.class, "Webcam 1");

        //  Instantiate the Vuforia engine
        vuforia = ClassFactory.getInstance().createVuforia(parameters);
    }

    /**
     * Initialize the TensorFlow Object Detection engine.
     */
    private void initTfod() {
        int tfodMonitorViewId = hardwareMap.appContext.getResources().getIdentifier(
                "tfodMonitorViewId", "id", hardwareMap.appContext.getPackageName());
        TFObjectDetector.Parameters tfodParameters = new TFObjectDetector.Parameters(tfodMonitorViewId);
        tfodParameters.minResultConfidence = 0.75f;
        tfodParameters.isModelTensorFlow2 = true;
        tfodParameters.inputSize = 300;
        tfod = ClassFactory.getInstance().createTFObjectDetector(tfodParameters, vuforia);

        // Use loadModelFromAsset() if the TF Model is built in as an asset by Android Studio
        // Use loadModelFromFile() if you have downloaded a custom team model to the Robot Controller's FLASH.
        tfod.loadModelFromAsset(TFOD_MODEL_ASSET, LABELS);
        // tfod.loadModelFromFile(TFOD_MODEL_FILE, LABELS);
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
            turnSpeed = Range.clip(turnSpeed * maxTurnSpeed, -maxTurnSpeed, maxTurnSpeed);

            // Pivot in place by applying the turning correction
            turnRobot(turnSpeed);

            // Display drive status for the driver.
            sendTelemetry();
        }

        // Stop all motion;
        stopAllMotion();
    }
    public void turnToHeading2(double heading, double minPower, double maxPower){
        robotHeading = getRawHeading() - headingOffset;
        headingError = heading - robotHeading;
        while (headingError > 180) headingError -= 360;
        while (headingError <= -180) headingError += 360;
        final double originalError = headingError;

        while(opModeIsActive() && (Math.abs(headingError) > HEADING_THRESHOLD)){
            robotHeading = getRawHeading() - headingOffset;
            headingError = heading - robotHeading;
            while (headingError > 180) headingError -= 360;
            while (headingError <= -180) headingError += 360;

            double adjuster = Math.abs(headingError / 180);
            double adjustedPower = Range.clip(adjuster, minPower, maxPower);

            if(headingError > 0){
                turnRobot(-adjustedPower);
            }
            else if(headingError < 0){
                turnRobot(adjustedPower);
            }
            telemetry.addData("heading", robotHeading);
            telemetry.addData("raw heading", getRawHeading());
            telemetry.addData("error", headingError);
            telemetry.addData("adjuster", adjuster);
            telemetry.addData("adjustedPower", adjustedPower);
            telemetry.update();
        }
        stopAllMotion();
    }

    /**
     * turns at 10% speed (or less) until reaching desired target
     * @param heading +ve turns the robot counterclockwise, -ve rotates clockwise
     */
    public void turnToHeading3(double heading){
        double currentHeading = getRawHeading();
        double errorABS = Math.abs(heading - currentHeading);
        while(errorABS > 1){
            double error = (heading - currentHeading) / 1800;
            while(error > 180) error -=360;
            while(error <= -180) error +=360;
            double speed = Range.clip(error, -0.1, 0.1);
            if(speed < 0.02 && speed > 0){
                speed = 0.02;
            }
            if(speed > -0.02 && speed < 0){
                speed = -0.02;
            }
            turnRobot(-speed);
            telemetry.addData("speed", speed);
            telemetry.addData("heading", heading);
            telemetry.addData("currentHeading", currentHeading);
            telemetry.addData("abs error", errorABS);
            telemetry.update();
            currentHeading = getRawHeading();
            errorABS = Math.abs(heading - currentHeading);
        }
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
            // turnSpeed = Range.clip(turnSpeed, -maxTurnSpeed, maxTurnSpeed);

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
        return 1 - 2 / (Math.exp(headingError / 30) + 1);
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
}