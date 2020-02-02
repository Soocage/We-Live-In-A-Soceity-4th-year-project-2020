package engine;

import org.lwjgl.glfw.*;

import static org.lwjgl.glfw.GLFW.*;


public class Input {

  private static boolean[] keys = new boolean[GLFW_KEY_LAST];
  private static boolean[] buttons = new boolean[GLFW_MOUSE_BUTTON_LAST];
  private static double mouseX, mouseY;
  private static double scrollX, scrollY;

  private static GLFWKeyCallback keyboard;
  private static GLFWCursorPosCallback mouseMove;
  private static GLFWMouseButtonCallback mouseButtons;
  private static GLFWScrollCallback mouseScroll;

  public Input() {
    keyboard = new GLFWKeyCallback() {
      @Override
      public void invoke(long window, int key, int scancode, int action, int mods) {
        keys[key] = (action != GLFW_RELEASE);

      }
    };

    mouseMove = new GLFWCursorPosCallback() {
      @Override
      public void invoke(long window, double xpos, double ypos) {
        mouseX = xpos;
        mouseY = ypos;
      }
    };

    mouseButtons = new GLFWMouseButtonCallback() {
      @Override
      public void invoke(long window, int button, int action, int mods) {
        buttons[button] = (action != GLFW.GLFW_RELEASE);
      }
    };

    mouseScroll = new GLFWScrollCallback() {
      @Override
      public void invoke(long window, double xoffset, double yoffset) {
        scrollX += xoffset;
        scrollY += yoffset;
      }
    };
  }

  public static boolean isKeyDown(int key) {
    return keys[key];
  }

  public static boolean isButtonDown(int button) {
    return buttons[button];
  }

  public static void destroy() {
    keyboard.free();
    mouseMove.free();
    mouseButtons.free();
    mouseScroll.free();
  }

  public static double getMouseX() {
    return mouseX;
  }

  public static double getMouseY() {
    return mouseY;
  }

  public GLFWScrollCallback getMouseScroll() {
    return mouseScroll;
  }

  public static double getScrollX() {
    return scrollX;
  }

  public static double getScrollY() {
    return scrollY;
  }

  public GLFWCursorPosCallback getMouseMove() {
    return mouseMove;
  }

  public GLFWKeyCallback getKeyboard() {
    return keyboard;
  }

  public GLFWMouseButtonCallback getMouseButtons() {
    return mouseButtons;
  }
}
