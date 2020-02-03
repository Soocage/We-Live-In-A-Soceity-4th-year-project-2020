package enginetest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import engine.Window;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class WindowTests {

  static Window window;
  static int[] heightArray = new int[] {768, 800, 624, 1002, 1070};
  static int[] widthArray = new int[] {640, 760, 1280, 1600, 240};
  static String[] titleArray = new String[] {"Test window 1", "Test window 2",
      "Test window 3", "Test window 4", "Test window 5"};

  @BeforeEach
  void setup() {
    window = new Window(heightArray[0], heightArray[0], titleArray[0]);
    window.create();
  }

  @Test
  public void windowHasCorrectAttributes() {
    for (int i = 0; i < heightArray.length; i++) {
      window.setSize(widthArray[i], heightArray[i]);
      window.setTitle(titleArray[i]);
      assertEquals(window.getHeight(), heightArray[i]);
      assertEquals(window.getWidth(), widthArray[i]);
      assertEquals(window.getTitle(), titleArray[i]);
    }
  }

  @Test
  public void fullscreenToggleTest() {
    assertFalse(window.isFullscreen());
    window.setFullscreen(true);
    System.out.println(window.isFullscreen());
    assertTrue(window.isFullscreen());
  }


}
