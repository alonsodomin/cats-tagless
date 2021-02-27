import com.typesafe.sbt.SbtGit.git

addCommandAlias("validateJVM", "all scalafmtCheckAll scalafmtSbtCheck testsJVM/test")
addCommandAlias("validateJS", "all testsJS/test")
addCommandAlias("fmt", "all scalafmtSbt scalafmtAll")
addCommandAlias("fmtCheck", "all scalafmtSbtCheck scalafmtCheckAll")
addCommandAlias("gitSnapshots", ";set version in ThisBuild := git.gitDescribedVersion.value.get + \"-SNAPSHOT\"")

val Scala212 = "2.12.13"
val Scala213 = "2.13.5"

val gitRepo = "git@github.com:typelevel/cats-tagless.git"
val homePage = "https://typelevel.org/cats-tagless"

// sbt-spiewak settings
ThisBuild / baseVersion := "0.12"
ThisBuild / publishGithubUser := "joroKr21"
ThisBuild / publishFullName := "Georgi Krastev"
enablePlugins(SonatypeCiReleasePlugin)

// update to scala 3 requires swapping from scalatest to munit and reimplementing all macros
ThisBuild / crossScalaVersions := Seq(Scala212, Scala213 /*, "3.0.0-M1", "3.0.0-M2"*/ )
ThisBuild / scalaVersion := Scala213
ThisBuild / githubWorkflowPublishTargetBranches := Nil
ThisBuild / githubWorkflowArtifactUpload := false
ThisBuild / githubWorkflowBuildMatrixAdditions += "ci" -> List("validateJS", "validateJVM")
ThisBuild / githubWorkflowBuild := List(WorkflowStep.Sbt(List("${{ matrix.ci }}"), name = Some("Validation")))
ThisBuild / githubWorkflowAddedJobs ++= Seq(
  WorkflowJob(
    "microsite",
    "Microsite",
    githubWorkflowJobSetup.value.toList ::: List(
      WorkflowStep.Use(
        UseRef.Public("ruby", "setup-ruby", "v1"),
        name = Some("Setup Ruby"),
        params = Map("ruby-version" -> "2.6", "bundler-cache" -> "true")
      ),
      WorkflowStep.Run(List("gem install jekyll -v 2.5"), name = Some("Install Jekyll")),
      WorkflowStep.Sbt(List("docs/makeMicrosite"), name = Some("Build microsite"))
    ),
    scalas = List(Scala213)
  )
)

val catsVersion = "2.4.2"
val catsEffectVersion = "2.3.3"
val circeVersion = "0.13.0"
val disciplineVersion = "1.1.4"
val disciplineScalaTestVersion = "2.1.2"
val paradiseVersion = "2.1.1"
val scalaCheckVersion = "1.15.3"
val shapelessVersion = "2.3.3"

val macroSettings = List(
  libraryDependencies ++=
    List("scala-compiler", "scala-reflect").map("org.scala-lang" % _ % scalaVersion.value % Provided),
  scalacOptions ++= (scalaBinaryVersion.value match {
    case "2.13" => List("-Ymacro-annotations")
    case _ => Nil
  }),
  libraryDependencies ++= (scalaBinaryVersion.value match {
    case "2.13" => Nil
    case _ => List(compilerPlugin(("org.scalamacros" %% "paradise" % paradiseVersion).cross(CrossVersion.full)))
  })
)

lazy val `cats-tagless` = project
  .aggregate(rootJVM, rootJS, docs)
  .dependsOn(rootJVM, rootJS)
  .settings(rootSettings, noPublishSettings)

lazy val rootJVM = project
  .aggregate(coreJVM, lawsJVM, testsJVM, macrosJVM)
  .dependsOn(coreJVM, lawsJVM, testsJVM, macrosJVM)
  .settings(rootSettings, noPublishSettings)

lazy val rootJS = project
  .aggregate(coreJS, lawsJS, testsJS, macrosJS)
  .dependsOn(coreJS, lawsJS, testsJS, macrosJS)
  .settings(rootSettings, noPublishSettings, commonJsSettings)

lazy val coreJVM = core.jvm
lazy val coreJS = core.js
lazy val core = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .enablePlugins(AutomateHeaderPlugin)
  .jsSettings(commonJsSettings)
  .settings(rootSettings)
  .settings(
    moduleName := "cats-tagless-core",
    libraryDependencies += "org.typelevel" %%% "cats-core" % catsVersion
  )

lazy val lawsJVM = laws.jvm
lazy val lawsJS = laws.js
lazy val laws = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .dependsOn(core)
  .enablePlugins(AutomateHeaderPlugin)
  .jsSettings(commonJsSettings)
  .settings(rootSettings)
  .settings(
    moduleName := "cats-tagless-laws",
    libraryDependencies ++= List(
      "org.typelevel" %%% "cats-laws" % catsVersion,
      "org.typelevel" %%% "discipline-core" % disciplineVersion
    )
  )

lazy val macrosJVM = macros.jvm
lazy val macrosJS = macros.js
lazy val macros = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .dependsOn(core)
  .aggregate(core)
  .enablePlugins(AutomateHeaderPlugin)
  .jsSettings(commonJsSettings)
  .settings(rootSettings, macroSettings, copyrightHeader)
  .settings(
    moduleName := "cats-tagless-macros",
    scalacOptions := scalacOptions.value.filterNot(_.startsWith("-Wunused")).filterNot(_.startsWith("-Ywarn-unused")),
    libraryDependencies += "org.scalacheck" %%% "scalacheck" % scalaCheckVersion % Test
  )

lazy val testsJVM = tests.jvm
lazy val testsJS = tests.js
lazy val tests = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .dependsOn(macros, laws)
  .enablePlugins(AutomateHeaderPlugin)
  .jsSettings(commonJsSettings)
  .settings(rootSettings, macroSettings, noPublishSettings)
  .settings(
    moduleName := "cats-tagless-tests",
    libraryDependencies ++= List(
      "org.typelevel" %%% "cats-free" % catsVersion,
      "org.typelevel" %%% "cats-testkit" % catsVersion,
      "org.typelevel" %%% "cats-effect" % catsEffectVersion,
      "io.circe" %%% "circe-core" % circeVersion,
      "org.typelevel" %%% "discipline-scalatest" % disciplineScalaTestVersion,
      "com.chuusai" %%% "shapeless" % shapelessVersion
    ).map(_ % Test)
  )

/** Docs - Generates and publishes the scaladoc API documents and the project web site. */
lazy val docs = project
  .dependsOn(List(macrosJVM).map(ClasspathDependency(_, Some("compile;test->test"))): _*)
  .enablePlugins(MicrositesPlugin, SiteScaladocPlugin)
  .settings(rootSettings, macroSettings, noPublishSettings)
  .settings(
    moduleName := "cats-tagless-docs",
    libraryDependencies += "org.typelevel" %%% "cats-free" % catsVersion,
    scalaVersion := Scala213,
    crossScalaVersions := Seq(Scala213),
    docsMappingsAPIDir := "api",
    addMappingsToSiteDir(mappings in packageDoc in Compile in coreJVM, docsMappingsAPIDir),
    organization := "org.typelevel",
    autoAPIMappings := true,
    micrositeName := "Cats-tagless",
    micrositeDescription := "A library of utilities for tagless final algebras",
    micrositeBaseUrl := "cats-tagless",
    micrositeGithubOwner := "typelevel",
    micrositeGithubRepo := "cats-tagless",
    micrositeHighlightTheme := "atom-one-light",
    micrositeTheme := "pattern",
    micrositePalette := Map(
      "brand-primary" -> "#51839A",
      "brand-secondary" -> "#EDAF79",
      "brand-tertiary" -> "#96A694",
      "gray-dark" -> "#192946",
      "gray" -> "#424F67",
      "gray-light" -> "#E3E2E3",
      "gray-lighter" -> "#F4F3F4",
      "white-color" -> "#FFFFFF"
    ),
    ghpagesNoJekyll := false,
    micrositeAuthor := "cats-tagless Contributors",
    scalacOptions -= "-Xfatal-warnings",
    git.remoteRepo := gitRepo,
    includeFilter in makeSite := "*.html" | "*.css" | "*.png" | "*.jpg" | "*.gif" | "*.js" | "*.swf" | "*.yml" | "*.md"
  )

lazy val docsMappingsAPIDir = settingKey[String]("Name of subdirectory in site target directory for api docs")
lazy val rootSettings = (organization := "org.typelevel") :: commonSettings ::: publishSettings

lazy val commonSettings = copyrightHeader ::: List(
  crossScalaVersions := (ThisBuild / crossScalaVersions).value,
  parallelExecution in Test := false,
  resolvers ++= Seq(Resolver.sonatypeRepo("releases"), Resolver.sonatypeRepo("snapshots")),
  addCompilerPlugin(("org.typelevel" % "kind-projector" % "0.11.3").cross(CrossVersion.full))
)

lazy val commonJsSettings = List(
  scalaJSStage in Global := FastOptStage,
  // currently sbt-doctest doesn't work in JS builds
  // https://github.com/tkawachi/sbt-doctest/issues/52
  doctestGenTests := Nil
)

lazy val noPublishSettings = List(
  skip in publish := true
)

lazy val publishSettings = List(
  homepage := Some(url(homePage)),
  scmInfo := Some(ScmInfo(url(homePage), gitRepo)),
  licenses := List("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.html")),
  developers := List(
    Developer(
      "Georgi Krastev",
      "@joroKr21",
      "joro.kr.21@gmail.com",
      url("https://www.linkedin.com/in/georgykr")
    ),
    Developer(
      "Kailuo Wang",
      "@kailuowang",
      "kailuo.wang@gmail.com",
      url("http://kailuowang.com")
    ),
    Developer(
      "Luka Jacobowitz",
      "@LukaJCB",
      "luka.jacobowitz@fh-duesseldorf.de",
      url("http://stackoverflow.com/users/3795501/luka-jacobowitz")
    )
  )
)

lazy val copyrightHeader = List(
  startYear := Some(2019),
  organizationName := "cats-tagless maintainers"
)
