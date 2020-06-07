package lunarlander;

import javafx.animation.*;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Path;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Shape;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;
import lunarlander.SidePane.UpdateLanderInfoEvent;


/**
 * Class preparing a Pane of a game screen we are
 * displaying in ApplicationWindow, {@link GameWindow}.
 */
public class GamePane {

  /**
   * Constructor reading configuration.json file, {@link Configuration},
   * and creating a Pane of moonSurface of chosen Moon, {@link Moon}.
   */
  public GamePane(Configuration configuration, Lander landerModel) {
    this.configuration = configuration;
    /*
     * for (int i = 1; i <= 3; i++) {
     *   this.configuration.generateLevel(i);
     * }
     * this.configuration.toFile();
     */

    if(!this.configuration.isConfigDownloaded()) {
      this.configuration.fromFile("src/main/resources/lunarlander/configuration.json");
    } else {
      this.configuration.fromFile("src/main/resources/lunarlander/configuration_fromserver.json");
    }

    Moon moon = this.configuration.getMoonMap(3);
    this.landingHeight = moon.getScaledLandingHeight();
    this.moonSurface = new Polygon();
    moonSurface.getPoints().addAll(moon.getMoonSurfacePoints());

    Group group = new Group();
    // TODO: Needs Lander placement based on window size!
    //this.landerModel = new Lander(0, 0, 0.1, 0, 0, 400);
    group.getChildren().addAll(moonSurface, landerModel.landerGroup);

    this.gamePane = new Pane();
    gamePane.getChildren().add(group);

    moonSurface.setFill(Color.LIGHTGRAY);
    this.gamePane.setStyle("-fx-background-color: black;");

    this.gamePane.widthProperty().addListener((observableValue, oldSceneWidth, newSceneWidth) -> {
      moon.recalculateWidth(newSceneWidth.doubleValue());
      moonSurface.getPoints().setAll(moon.getMoonSurfacePoints());
      this.landingHeight = moon.getScaledLandingHeight();
    });
    this.gamePane.heightProperty().addListener((observableValue, oldSceneHeight, newSceneHeight) -> {
      moon.recalculateHeight(newSceneHeight.doubleValue());
      moonSurface.getPoints().setAll(moon.getMoonSurfacePoints());
      this.landingHeight = moon.getScaledLandingHeight();
    });


    TranslateTransition vertical = new TranslateTransition(Duration.millis(32), landerModel.landerGroup);
    vertical.setInterpolator(Interpolator.LINEAR);

    RotateTransition leftRotate = new RotateTransition(Duration.millis(32), landerModel.landerGroup);
    RotateTransition rightRotate = new RotateTransition(Duration.millis(32), landerModel.landerGroup);

    KeyFrame keyframe = new KeyFrame(Duration.millis(32), event -> {

      landerModel.xCoord = landerModel.landerGroup.getLayoutX() + landerModel.landerGroup.getTranslateX();
      landerModel.yCoord = landerModel.landerGroup.getLayoutY() + landerModel.landerGroup.getTranslateY();

      if (((Path)Shape.intersect(landerModel.lander, this.moonSurface)).getElements().size() > 0) {
        timeline.stop();
        // TODO: Game ending
      }

      if (isLeftRotate() && !isRightRotate() && landerModel.angle >= -150) {
        leftRotate.setAxis(Rotate.Z_AXIS);
        leftRotate.setByAngle(-4);
        landerModel.angle = landerModel.angle-4;
        leftRotate.setAutoReverse(false);
        leftRotate.play();
      }
      if (isRightRotate() && !isLeftRotate() && landerModel.angle <= 150) {
        rightRotate.setAxis(Rotate.Z_AXIS);
        rightRotate.setByAngle(4);
        landerModel.angle = landerModel.angle+4;
        rightRotate.setAutoReverse(false);
        rightRotate.play();
      }

      if(isThrustON() && landerModel.fuel > 0) {
        landerModel.ax = Math.sin(landerModel.angle * (Math.PI / 180)) * 0.1;
        landerModel.ay = Math.cos(landerModel.angle * (Math.PI / 180)) * 0.2;
        landerModel.vy = landerModel.vy + g - landerModel.ay;
        landerModel.vx = landerModel.vx + landerModel.ax;
        landerModel.fuel = landerModel.fuel-0.25;
        landerModel.setFlameImage(Lander.FlameImageType.FLAME);
        System.out.println(landerModel.fuel);
      } else {
        landerModel.vy = landerModel.vy + g;
        landerModel.setFlameImage(Lander.FlameImageType.NO_FLAME);
      }

      if(!isThrustON() || landerModel.fuel == 0) {
        landerModel.setFlameImage(Lander.FlameImageType.NO_FLAME);
      }

      landerModel.v = Math.sqrt(Math.pow(landerModel.vx, 2)+Math.pow(landerModel.vy, 2));

      this.gamePane.fireEvent(new SidePane.UpdateLanderInfoEvent(
            landerModel.fuel,
            landerModel.v,
            this.landingHeight - landerModel.getBottomCoord()));
      // TODO: MOOOOOOORE THINGS

      vertical.setByY(landerModel.vy);
      vertical.setByX(landerModel.vx);
      vertical.stop();
      vertical.play();
    });

    timeline.getKeyFrames().add(keyframe);
    timeline.setCycleCount(Animation.INDEFINITE);
    timeline.play();
  }

  /*
   * Event handling in methods below
   * Event triggering in GamePane setOnKeyPressed and setOnKeyReleased.
   */
  public void startLanderThrustOn() { this.isThrustON = true; }

  public void stopLanderThrustOn() { this.isThrustON = false; }

  public void startRotateLanderClockwise() { this.isRightRotate = true; }

  public void stopRotateLanderClockwise() { this.isRightRotate = false; }

  public void startRotateLanderAnticlockwise() { this.isLeftRotate = true; }

  public void stopRotateLanderAnticlockwise() { this.isLeftRotate = false; }

  public void pauseGame() {
    this.isPaused = true;
    this.timeline.pause();
  }

  public void unpauseGame() {
    this.isPaused = false;
    this.timeline.play();
  }


  /**
   * Getter used in ApplicationWindow class in order to put
   * prepared Pane in a Scene.
   *
   * @return gamePane - Pane presenting game view.
   */
  public Pane getGamePane() { return gamePane; }

  public boolean isLeftRotate() { return isLeftRotate; }

  public boolean isRightRotate() { return isRightRotate; }

  public boolean isThrustON() { return isThrustON; }

  public boolean isPaused() { return isPaused; }

  private Pane gamePane;
  private Lander landerModel;
  private Timeline timeline = new Timeline();
  private Polygon moonSurface;
  private double landingHeight;

  private boolean isLeftRotate = false;
  private boolean isRightRotate = false;
  private boolean isThrustON = false;
  private boolean isPaused = false;
  private double g = 0.1;
  private double v;

  private Configuration configuration;

  //TODO: Nitro flame
}
