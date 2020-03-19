package game.menu;

import engine.Window;
import engine.graphics.Material;
import engine.graphics.mesh.dimension.two.RectangleMesh;
import engine.graphics.renderer.GuiRenderer;
import engine.graphics.renderer.TextRenderer;
import engine.graphics.text.Text;
import engine.io.Input;
import engine.objects.gui.ButtonObject;
import engine.objects.world.Camera;
import engine.utils.ColourUtils;
import game.Game;
import game.GameState;
import game.world.Gui;
import game.world.World;
import math.Vector3f;
import org.jfree.chart.ChartColor;
import org.lwjgl.glfw.GLFW;

public class MainMenu {
  private static final Vector3f BACKGROUND_COLOUR = ColourUtils.convertColor(
      ChartColor.VERY_LIGHT_CYAN.brighter());
  private static final float BUTTON_FONT_SIZE = 2f;
  private static float BUTTON_WIDTH = 0.7f;
  private static float BUTTON_HEIGHT = 0.3f;
  private static String BUTTON_TEXTURE = "/images/button_texture.jpg";
  private static float[] START_REPOSITION_VALUES = {0, 0, 1, -0.6f};
  private static float[] EXIT_REPOSITION_VALUES = {0, 0, -1, 0.6f};
  private static ButtonObject startButton;
  private static ButtonObject exitButton;
  private static ButtonObject[] buttons = new ButtonObject[2];

  public static void create(Window window) {
    setBackgroundColour(BACKGROUND_COLOUR, window);
    createButtons();
  }

  /**
   * Update.
   *
   * @param window the window
   */
  public static void update(Window window, Camera camera) {
    resize();
    updateButtons(window);
    checkButtonClick(window, camera);
  }

  /**
   * Destroy.
   */
  public static void destroy() {
    // Destroy Buttons
    for (ButtonObject button : buttons) {
      button.destroy();
    }
  }


  private static void updateButtons(Window window) {
    for (ButtonObject button : buttons) {
      button.update(window);
    }
  }

  /**
   * Check button click.
   *
   * @param window the window
   */
  public static void checkButtonClick(Window window, Camera camera) {
    if (Input.isButtonDown(GLFW.GLFW_MOUSE_BUTTON_LEFT)) {
      if (startButton.isMouseOver(window)) {
        // Change the Game State
        Game.setState(GameState.GAME);
        // Destroy the Main Menu
        destroy();
        // Create the Gui
        Gui.create();
        // Create the World
        World.create(camera);
      } else if (exitButton.isMouseOver(window)) {
        GLFW.glfwSetWindowShouldClose(window.getWindow(), true);
      }
    }
  }

  public static ButtonObject[] getButtonObjects() {
    return buttons.clone();
  }

  private static void createButtons() {
    // Create Start Button
    Text startButtonText = new Text("Start", BUTTON_FONT_SIZE);
    startButtonText.setCentreHorizontal(true);
    startButtonText.setCentreVertical(true);
    startButton = initButton(startButtonText, START_REPOSITION_VALUES[0],
        START_REPOSITION_VALUES[1], START_REPOSITION_VALUES[2], START_REPOSITION_VALUES[3]);
    startButton.create();
    buttons[0] = startButton;
    // Create Exit Button
    Text exitButtonText = new Text("Exit", BUTTON_FONT_SIZE);
    exitButtonText.setCentreHorizontal(true);
    exitButtonText.setCentreVertical(true);
    exitButton = initButton(exitButtonText, EXIT_REPOSITION_VALUES[0],
        EXIT_REPOSITION_VALUES[1], EXIT_REPOSITION_VALUES[2], EXIT_REPOSITION_VALUES[3]);
    exitButton.create();
    buttons[1] = exitButton;
  }

  /**
   * Render.
   *
   * @param guiRenderer  the gui renderer
   * @param textRenderer the text renderer
   */
  public static void render(GuiRenderer guiRenderer, TextRenderer textRenderer) {
    startButton.render(guiRenderer, textRenderer);
    exitButton.render(guiRenderer, textRenderer);
  }

  public static void resize() {
    startButton.reposition();
    exitButton.reposition();
  }

  private static void setBackgroundColour(Vector3f colour, Window window) {
    window.setBackgroundColour(colour.getX(), colour.getY(), colour.getZ(), 1f);
  }

  private static ButtonObject initButton(
      Text buttonText,
      float edgeX,
      float offsetX,
      float edgeY,
      float offsetY) {
    RectangleMesh tempMesh = new RectangleMesh(BUTTON_WIDTH, BUTTON_HEIGHT,
        new Material(BUTTON_TEXTURE));

    return new ButtonObject(tempMesh, buttonText, edgeX, offsetX, edgeY, offsetY);
  }
}