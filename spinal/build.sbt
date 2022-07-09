import scala.sys.process._

lazy val buildMachineCode = taskKey[Unit]("Execute ASMotor")

val spinalVersion = "1.6.4"

lazy val r8r = (project in file("."))
	.settings(
		name := "R8R",

		version := "1.0",

		scalaVersion := "2.12.11",

  		libraryDependencies ++= Seq(
			"com.github.spinalhdl" % "spinalhdl-core_2.12" % spinalVersion,
			"com.github.spinalhdl" % "spinalhdl-lib_2.12" % spinalVersion,
			compilerPlugin("com.github.spinalhdl" % "spinalhdl-idsl-plugin_2.12" % spinalVersion)
		),

		fork := true,

		buildMachineCode := {
			val s: TaskStreams = streams.value
			val shell: Seq[String] = if (sys.props("os.name").contains("Windows")) Seq("cmd", "/c") else Seq("bash", "-c")
			val buildr8r: Seq[String] = shell :+ "motorrc8 -fb -ocode.bin code.rc8"
			s.log.info("Building machine code...")
			if ((buildr8r !) == 0) {
				s.log.success("Machine code build successful!")
			} else {
				throw new IllegalStateException("Machine code build failed!")
			}
		},

		(run in Compile) := ((run in Compile) dependsOn buildMachineCode).evaluated
	)