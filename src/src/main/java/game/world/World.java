package game.world;

import engine.Window;
import engine.audio.AudioMaster;
import engine.graphics.Material;
import engine.graphics.image.Image;
import engine.graphics.mesh.dimension.two.RectangleMesh;
import engine.graphics.model.dimension.two.RectangleModel;
import engine.graphics.renderer.WorldRenderer;
import engine.io.Input;
import engine.objects.world.Camera;
import engine.objects.world.GameObject;
import engine.objects.world.TileWorldObject;
import engine.tools.MousePicker;
import engine.utils.ArrayUtils;
import engine.utils.ColourUtils;
import game.Game;
import game.GameState;
import game.menu.ChoiceMenu;
import game.menu.DealingMenu;
import game.menu.TradingMenu;
import game.menu.data.TradeDeal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import map.MapGeneration;
import map.tiles.AridTile;
import map.tiles.FertileTile;
import map.tiles.PlainTile;
import map.tiles.Tile;
import map.tiles.WaterTile;
import math.Vector2f;
import math.Vector3f;
import math.Vector4f;
import neat.Client;
import org.jfree.chart.ChartColor;
import org.lwjgl.glfw.GLFW;
import society.Society;

public class World {
  private static final float LOWER_VERTEX_BAND = -0.5f;
  private static final float UPPER_VERTEX_BAND = 0.5f;
  private static final float DEFAULT_Z = 0;
  private static final Vector3f DEFAULT_ROTATION = new Vector3f(0, 0, 0);
  private static final Vector3f DEFAULT_SCALE = new Vector3f(1f, 1f, 1f);
  private static final int DEFAULT_NUMBER_OF_SOCIETIES = 4;
  private static final Vector3f[] BASIC_SOCIETY_COLORS = new Vector3f[] {
      ColourUtils.convertColor(ChartColor.RED),
      ColourUtils.convertColor(ChartColor.LIGHT_YELLOW),
      ColourUtils.convertColor(ChartColor.LIGHT_GREEN),
      ColourUtils.convertColor(ChartColor.LIGHT_CYAN)};
  private static final int FERTILE_MIN_FOOD_RESOURCE = 6;
  private static final int FERTILE_MAX_FOOD_RESOURCE = 9;
  private static final int FERTILE_MAX_RAW_MATERIALS = 4;
  private static final int FERTILE_MIN_RAW_MATERIALS = 3;
  private static final int ARID_MAX_FOOD_RESOURCE = 4;
  private static final int ARID_MIN_FOOD_RESOURCE = 3;
  private static final int ARID_MAX_RAW_MATERIALS = 9;
  private static final int ARID_MIN_RAW_MATERIALS = 6;
  private static final int PLAIN_MAX_FOOD_RESOURCE = 6;
  private static final int PLAIN_MIN_FOOD_RESOURCE = 5;
  private static final int PLAIN_MAX_RAW_MATERIALS = 5;
  private static final int PLAIN_MIN_RAW_MATERIALS = 4;
  private static final int WATER_MAX_FOOD_RESOURCE = 2;
  private static final int WATER_MAX_RAW_MATERIALS = 2;
  private static int numberOfSocieties;
  private static TileWorldObject[][] worldMap;
  private static ArrayList<GameObject> fertileTiles = new ArrayList<>();
  private static ArrayList<GameObject> aridTiles = new ArrayList<>();
  private static ArrayList<GameObject> plainTiles = new ArrayList<>();
  private static ArrayList<GameObject> waterTiles = new ArrayList<>();
  private static GameObject selectOverlay;
  private static Image selectOverlayImage = new Image("/images/blankFace.png");
  private static Vector4f overlayColour = new Vector4f(new Vector3f(1, 1, 1), 0.5f);
  private static Material selectOverlayMaterial = new Material(selectOverlayImage, overlayColour);
  private static boolean bordersAltered = false;
  private static Society[] societies = new Society[] {};
  private static ArrayList<Society> activeSocieties = new ArrayList<>();
  private static RectangleModel tileModel;
  private static TileWorldObject attackingTile;
  private static TileWorldObject opponentTile;
  private static TileWorldObject claimedTile;
  private static Society activeSociety;
  private static Society targetSociety;
  private static Society bestTradingCandidate;

  public static Society getBestTradingCandidate() {
    return bestTradingCandidate;
  }

  public static Society getTargetSociety() {
    return targetSociety;
  }

  public static RectangleModel getTileModel() {
    return tileModel;
  }

  public static Society[] getSocieties() {
    return societies.clone();
  }

  /**
   * Create.
   *
   * @param camera the camera
   */
  public static void create(Window window, Camera camera) {
    if (!Game.isTraining()) {
      create(window, camera, DEFAULT_NUMBER_OF_SOCIETIES);
    } else {
      if (Game.getTrainingMode() == 0) {
        numberOfSocieties = 1;
        create(window, camera, numberOfSocieties);
      } else {
        numberOfSocieties = DEFAULT_NUMBER_OF_SOCIETIES;
        create(window, camera, DEFAULT_NUMBER_OF_SOCIETIES);
      }
    }
  }

  /**
   * Create.
   *
   * @param window            the window
   * @param camera            the camera
   * @param numberOfSocieties the number of societies
   */
  public static void create(Window window, Camera camera, int numberOfSocieties) {
    MapGeneration.createMap();
    // reset the camera to its default position.
    camera.reset();
    // unfreeze the camera in-case it has been frozen before
    camera.unfreeze();
    createObjects(camera);
    // create the Mouse Picker
    MousePicker.setCamera(camera);
    MousePicker.setProjectionMatrix(window.getProjectionMatrix());
    MousePicker.setGroundZ(DEFAULT_Z);
    generateSocieties(numberOfSocieties);
  }

  private static void generateSocieties(int numberOfSocieties) {
    societies = new Society[numberOfSocieties];
    activeSocieties.clear();
    Client decisionClient = null;

    if (Game.isTraining()) {
      ArrayList<Client> decisionClients = Game.getNeat().getClients().getData();
      int clientIndex = Game.getDecisionClientIndex();
      decisionClient = decisionClients.get(clientIndex);
    } else {
      // A normal game is occurring
      try {
        decisionClient = Game.getNeat().getBestClient();
      } catch (NullPointerException e) {
        // Need this to fix unit tests
        System.out.println("Can't set the decision client");
      }
    }


    for (int i = 0; i < numberOfSocieties; i++) {
      Society society = new Society(i, BASIC_SOCIETY_COLORS[i]);
      societies[i] = society;

      // If training set the client for the society
      if (Game.isTraining()) {
        if (Game.getTrainingMode() == 0) {
          society.setDecisionClient(decisionClient);
        } else {
          if (society.getSocietyId() == 0) {
            society.setDecisionClient(decisionClient);
          } else {
            society.setDecisionClient(Game.getNeat().getRandomClient());
          }
        }
      } else {
        // else set the client to be the best from the NEAT
        society.setDecisionClient(decisionClient);
      }

      activeSocieties.add(society);
      boolean claimed = false;
      while (!claimed) {
        int row = genRandomInt(worldMap[0].length - 2, 1);
        int column = genRandomInt(worldMap[0].length - 2, 1);
        if (!worldMap[row][column].isClaimed()
            && !(worldMap[row][column].getTile() instanceof WaterTile)) {
          activeSocieties.get(i).claimTile(worldMap[row][column]);
          bordersAltered = true;
          claimed = true;
        }
      }
    }
  }

  /**
   * Render the game tiles and render the select overlay if mouse is not locked.
   *
   * @param renderer the renderer
   * @param camera   the camera
   * @param window   the window
   */
  public static void render(WorldRenderer renderer, Camera camera, Window window) {
    renderTiles(renderer, camera);
    if (MousePicker.getCurrentSelected() != null && !window.isMouseLocked()) {
      renderer.renderSelectOverlay(selectOverlay, camera);
    }
    renderBorder(renderer, camera);
  }

  private static void renderBorder(WorldRenderer renderer, Camera camera) {
    ArrayList<GameObject> temp = new ArrayList<>();
    for (Society society : activeSocieties) {
      for (TileWorldObject worldTile : society.getTerritory()) {
        temp.add(worldTile.getBorderObject());
      }
    }
    renderer.renderTileBorders(temp, camera);
  }

  public static TileWorldObject getAttackingTile() {
    return attackingTile;
  }

  public static TileWorldObject getOpponentTile() {
    return opponentTile;
  }

  /**
   * Update the world including the openAl listener.
   *
   * @param window the window
   */
  public static void update(Window window, Camera camera) {
    AudioMaster.setListener(camera.getPosition());
    updateBorders(window);
    if (Game.getState() == GameState.WARRING) {
      selectWarTiles(window);
    } else if (Game.getState() == GameState.CLAIM_TILE) {
      selectClaimableTile(window);
    }
  }

  private static void selectClaimableTile(Window window) {
    if (claimedTile == null) {
      MousePicker.update(window, societies[0].getClaimableTerritory());
      updateSelectOverlay();
      claimedTile = selectWorldTile(societies[0].getClaimableTerritory());
    } else {
      societies[0].claimTile(claimedTile);
      bordersAltered = true;
      updateSocietyBorders();
      claimedTile = null;
      societies[0].setEndTurn(true);
      Game.setState(GameState.GAME_MAIN);
    }
  }

  private static void selectWarTiles(Window window) {
    if (attackingTile == null) {
      MousePicker.update(window, societies[0].getAttackingTiles());
      updateSelectOverlay();
      attackingTile = selectWorldTile(societies[0].getAttackingTiles());
    } else if (opponentTile == null) {
      activeSocieties.get(0).calculateDefendingTiles(attackingTile);
      MousePicker.update(window, societies[0].getDefendingTiles());
      updateSelectOverlay();
      opponentTile = selectWorldTile(societies[0].getDefendingTiles());
    } else {
      simulateBattle(societies[0], attackingTile, opponentTile);
      societies[0].setEndTurn(true);
      attackingTile = null;
      opponentTile = null;
    }
  }

  private static TileWorldObject selectWorldTile(ArrayList<TileWorldObject> worldTiles) {
    if (MousePicker.getCurrentSelected() != null
        && Input.isButtonDown(GLFW.GLFW_MOUSE_BUTTON_LEFT)
        && worldTiles.contains(MousePicker.getCurrentSelected())
        && Game.buttonLockFree()) {
      Game.resetButtonLock();
      return MousePicker.getCurrentSelected();
    } else {
      return null;
    }
  }

  /**
   * Update the world.
   *
   * @param window the window
   */
  public static void updateBorders(Window window) {
    if (worldMap != null) {
      updateSocietyBorders();
      MousePicker.update(window, worldMap);
      updateSelectOverlay();
    }
  }

  private static void updateSocietyBorders() {
    if (bordersAltered) {
      for (Society society : societies) {
        society.updateBorders(tileModel);
      }
      bordersAltered = false;
    }
  }

  private static void updateSelectOverlay() {
    // Check if the picker is currently selecting an object and if it is, update it's position
    if (MousePicker.getCurrentSelected() != null) {
      // reposition selectOverlay to tile's position
      selectOverlay.setPosition(MousePicker.getCurrentSelected().getPosition().copy());
    }
  }

  public static TileWorldObject[][] getWorldMap() {
    return worldMap.clone();
  }

  private static void renderTiles(WorldRenderer renderer, Camera camera) {
    renderer.renderTiles(fertileTiles, camera);
    renderer.renderTiles(aridTiles, camera);
    renderer.renderTiles(plainTiles, camera);
    renderer.renderTiles(waterTiles, camera);
  }

  private static void createObjects(Camera camera) {
    // calculate tile size based on vertex values
    float tileSize = UPPER_VERTEX_BAND + Math.abs(LOWER_VERTEX_BAND);
    createTileObjects(tileSize, camera);
  }

  private static void createTileObjects(float tileSize, Camera camera) {
    // left edge = the position of the first tile in the X axis. Starting left most edge
    float leftXEdge = calcLeftPos(MapGeneration.getLandMassSizeX(), tileSize);
    // top edge = the position of the first tile in the Y axis. Starting at the top most edge
    float topYEdge = calcTopPos(MapGeneration.getLandMassSizeY(), tileSize);
    // initialise 2d representation of the map
    tileModel = new RectangleModel(tileSize, tileSize);
    // create the select overlay
    selectOverlay = new GameObject(new RectangleMesh(tileModel, selectOverlayMaterial));
    selectOverlay.create();
    worldMap = new TileWorldObject[MapGeneration.getMapSizeX()][MapGeneration.getMapSizeY()];
    Tile[][] tileMap = MapGeneration.getSimulationMap();
    for (int row = 0; row < MapGeneration.getMapSizeY(); row++) {
      for (int column = 0; column < MapGeneration.getMapSizeX(); column++) {
        // create a tileWorldObject
        RectangleMesh tileMesh = new RectangleMesh(tileModel);
        Tile tile = tileMap[row][column];
        TileWorldObject tempTileWorldObject = new TileWorldObject(
            new Vector3f(leftXEdge + (tileSize * (float) column),
                topYEdge - (tileSize * (float) row), DEFAULT_Z), DEFAULT_ROTATION, DEFAULT_SCALE,
            tileMesh, tile, row, column);
        generateResources(tempTileWorldObject);
        // assign tile ot the world map
        worldMap[row][column] = tempTileWorldObject;
        // Create the Object
        if (!Game.isTraining()) {
          tempTileWorldObject.create();
        }
        // Add to Current Tile List
        addTileObject(tempTileWorldObject);
      }
    }
    // calculate the positions for the camera borders based on tiles in appropriate corners
    Vector2f botLeft = calcCentre(worldMap[worldMap.length - 1][0]);
    Vector2f topRight = calcCentre(worldMap[0][worldMap.length - 1]);
    // set camera borders
    camera.setCameraBorder(botLeft, topRight);
  }

  /**
   * Generate resources.
   *
   * @param tempTileWorldObject A TileWorldObject
   */
  public static void generateResources(TileWorldObject tempTileWorldObject) {
    if (tempTileWorldObject.getTile() instanceof WaterTile) {
      tempTileWorldObject.setFoodResource(genRandomInt(WATER_MAX_FOOD_RESOURCE));
      tempTileWorldObject.setRawMaterialResource(WATER_MAX_RAW_MATERIALS);
    } else if (tempTileWorldObject.getTile() instanceof FertileTile) {
      tempTileWorldObject.setFoodResource(genRandomInt(FERTILE_MAX_FOOD_RESOURCE,
          FERTILE_MIN_FOOD_RESOURCE));
      tempTileWorldObject.setRawMaterialResource(genRandomInt(FERTILE_MAX_RAW_MATERIALS,
          FERTILE_MIN_RAW_MATERIALS));
    } else if (tempTileWorldObject.getTile() instanceof AridTile) {
      tempTileWorldObject.setFoodResource(genRandomInt(ARID_MAX_FOOD_RESOURCE,
          ARID_MIN_FOOD_RESOURCE));
      tempTileWorldObject.setRawMaterialResource(genRandomInt(ARID_MAX_RAW_MATERIALS,
          ARID_MIN_RAW_MATERIALS));
    } else if (tempTileWorldObject.getTile() instanceof PlainTile) {
      tempTileWorldObject.setFoodResource(genRandomInt(PLAIN_MAX_FOOD_RESOURCE,
          PLAIN_MIN_FOOD_RESOURCE));
      tempTileWorldObject.setRawMaterialResource(genRandomInt(PLAIN_MAX_RAW_MATERIALS,
          PLAIN_MIN_RAW_MATERIALS));
    }
  }

  // function that returns Vector2f positions which are the centres of the tiles
  private static Vector2f calcCentre(TileWorldObject tile) {
    return new Vector2f(tile.getPosition().getX(), tile.getPosition().getY());
  }

  // calculates the left most position of the first tile
  private static float calcLeftPos(int mapDimension, float tileSize) {
    float value = 0;
    for (int i = 0; i < mapDimension / 2; i++) {
      value -= tileSize;
    }
    return value;
  }

  // calculates the top most position of the first tile
  private static float calcTopPos(int mapDimension, float tileSize) {
    float value = 0;
    for (int i = 0; i < mapDimension / 2; i++) {
      value += tileSize;
    }
    return value;
  }

  private static void addTileObject(TileWorldObject object) {
    Tile tile = object.getTile();
    if (tile instanceof FertileTile) {
      fertileTiles.add(object);
    } else if (tile instanceof AridTile) {
      aridTiles.add(object);
    } else if (tile instanceof WaterTile) {
      waterTiles.add(object);
    } else {
      plainTiles.add(object);
    }
  }

  public static int genRandomInt(int maxValue) {
    Random r = new Random();
    return r.nextInt(maxValue);
  }

  /**
   * Generate a random integer.
   *
   * @param maxValue the max value
   * @param minValue the min value
   * @return the int
   */
  public static int genRandomInt(int maxValue, int minValue) {
    Random r = new Random();
    int val = r.nextInt(maxValue + 1);
    return (val == minValue ? minValue : Math.max(val, minValue));
  }

  public static int getDefaultNumberOfSocieties() {
    return DEFAULT_NUMBER_OF_SOCIETIES;
  }

  /**
   * Destroy the world elements and clear the arrays.
   */
  public static void destroy() {
    // Destroy tile data
    if (!Game.isTraining()) {
      for (TileWorldObject[] row : worldMap) {
        for (TileWorldObject tile : row) {
          GameObject border = tile.getBorderObject();
          if (border != null) {
            border.destroy();
          }
          tile.destroy();
        }
      }
    }
    worldMap = null;
    fertileTiles.clear();
    waterTiles.clear();
    aridTiles.clear();
    plainTiles.clear();
    selectOverlay.destroy();
    // Destroy Overlay
    selectOverlay.destroy();
    if (!Game.isTraining()) {
      ChoiceMenu.destroy();
    }
    activeSocieties.clear();
    activeSociety = null;
  }

  private static void simulateBattle(Society attackingSociety,
                                     TileWorldObject playerTile, TileWorldObject opponentTile) {
    Society defendingSociety = opponentTile.getClaimedBy();
    float attackSocietyA = calcAttack(attackingSociety, playerTile);
    float attackSocietyB = calcAttack(defendingSociety, opponentTile);
    float attackingSocietyHappinessModifier = 1f;
    float defendingSocietyHappinessModifier = 1f;
    if (attackSocietyA > attackSocietyB) {
      // SocietyA wins the battle
      defendingSociety.getTerritory().remove(opponentTile);
      attackingSociety.claimTile(opponentTile);
      bordersAltered = true;
      // apply limit to attackingSocietyModifier
      attackingSocietyHappinessModifier = attackingSociety.limitHappinessModifier(
          (attackingSocietyHappinessModifier + attackingSociety.getAverageAggressiveness()));
      // increase happiness for attacking society
      attackingSociety.setHappiness(attackingSociety.getHappiness()
          * attackingSocietyHappinessModifier);
      // decrease happiness for defending society
      defendingSociety.setHappiness(defendingSociety.getHappiness()
          * defendingSociety.getAverageAggressiveness());

    } else if (attackSocietyA < attackSocietyB) {
      // SocietyB wins the battle
      attackingSociety.getTerritory().remove(playerTile);
      defendingSociety.claimTile(playerTile);
      bordersAltered = true;
      // decrease attacking society happiness
      attackingSociety.setHappiness(attackingSociety.getHappiness()
          * attackingSociety.getAverageAggressiveness());
      // apply limit to defendingSocietyHappinessModifier
      defendingSocietyHappinessModifier = defendingSociety.limitHappinessModifier(
          defendingSocietyHappinessModifier + defendingSociety.getAverageAggressiveness());
      // increase defending society happiness
      defendingSociety.setHappiness(defendingSociety.getHappiness()
          * defendingSocietyHappinessModifier);
    }
    purgeSocieties();
    Game.setState(GameState.GAME_MAIN);
  }

  private static void purgeSocieties() {
    List<Society> societiesToRemove = new ArrayList<>();
    // Find Societies to remove if any
    for (Society society : activeSocieties) {
      if (society.getTerritory().size() < 1) {
        societiesToRemove.add(society);
      }
    }
    // Remove the societies
    for (Society society : societiesToRemove) {
      activeSocieties.remove(society);
    }
  }

  private static float calcAttack(Society currentSociety, TileWorldObject worldTile) {
    // TODO NORMALISE THESE VALUES FOR THE PURPOSE OF BALANCING
    float armyAggressiveness = currentSociety.calcArmyAggression();
    float productionModifier = currentSociety.getAverageProductivity();
    float tileModifier = worldTile.getTile().getAttackModifier();
    return armyAggressiveness * productionModifier * tileModifier;
  }

  public static Society getActiveSociety() {
    return activeSociety;
  }

  public static void setActiveSociety(Society activeSociety) {
    World.activeSociety = activeSociety;
  }

  private static int getMoveID(Society society) {
    float largestPopulation = 1;
    for (Society soc : getActiveSocieties()) {
      if (soc.getPopulation().size() > largestPopulation) {
        largestPopulation = soc.getPopulation().size();
      }
    }
    // This function returns a turn ID where each number corresponds to a certain move

    // This will be returned by the NN;
    double[] moveWeights = new double[4];

    if (society.getDecisionClient() == null) {
      // Do random moves
      for (int i = 0; i < moveWeights.length; i++) {
        moveWeights[i] = Math.random();
      }
    } else {
      float sizeOfActiveSocieties = 1;
      if (World.getActiveSocieties().size() - 1 > 0) {
        sizeOfActiveSocieties = World.getActiveSocieties().size() - 1;
      }
      double[] inputs = new double[9];
      // Pass inputs
      inputs[0] = society.getAverageProductivity();
      inputs[1] = society.getAverageAggressiveness();
      inputs[2] = society.getTerritory().size() / (float) (MapGeneration.getLandMassSizeX()
          * MapGeneration.getLandMassSizeY());
      inputs[3] = society.getPopulation().size() / largestPopulation;
      inputs[4] = society.getArmy().size() / (float) society.getPopulation().size();
      inputs[5] = society.getTotalFoodResource()
          / (society.getPopulation().size() * Society.getFoodPerPerson());
      inputs[6] = society.getTotalRawMaterialResource()
          / (society.getPopulation().size() * Society.getMaterialPerPerson());
      society.calculateNeighbouringSocieties();
      inputs[7] = society.getNeighbouringSocieties().size()
          / sizeOfActiveSocieties;
      inputs[8] = society.getTradingSocieties().size()
          / sizeOfActiveSocieties;

      moveWeights = society.getDecisionClient().calculate(inputs);
    }

    // Create a map that will map a move ID to its weight
    TreeMap<Double, Integer> map = new TreeMap<>();

    for (int i = 0; i < moveWeights.length; i++) {
      map.put(moveWeights[i], i);
    }

    ArrayList<Integer> moveOrder = new ArrayList<>();
    for (Map.Entry<Double, Integer> entry : map.entrySet()) {
      moveOrder.add(entry.getValue());
    }

    int move = 0;
    for (int i = moveOrder.size() - 1; i >= 0; i--) {
      // Get the move ID
      move = moveOrder.get(i);

      if (move == 0) {

        // See if Claiming is possible
        society.calculateClaimableTerritory();
        if (!society.getClaimableTerritory().isEmpty()) {
          break;
        } else {
          society.incrementWrongMoves();
        }
      } else if (move == 1) {

        // See if Warring is possible
        // Get a list of valid tiles that we can attack
        ArrayList<TileWorldObject> validTiles = society.getValidTilesToAttack();
        if (!validTiles.isEmpty()) {
          break;
        } else {
          society.incrementWrongMoves();
        }
      } else if (move == 2) {

        // See if Trading is possible
        // calculate all possible societies you can trade with
        society.calculateNeighbouringSocieties();
        // Get a list of valid tiles that we can attack
        ArrayList<TileWorldObject> validTiles = society.getValidTilesToAttack();
        if (!society.getNeighbouringSocieties().isEmpty()) {
          // find best candidate for trading
          float foodPerPerson = 0;
          float matsPerPerson = 0;
          // calculate the society with highest food and raw materials per person
          // this will give rise to highest possibility of accepting a trade deal
          for (Society soc : society.getNeighbouringSocieties()) {
            if ((float) soc.getTotalFoodResource() / soc.getPopulation().size() > foodPerPerson
                && (float) soc.getTotalRawMaterialResource() / soc.getPopulation().size()
                > matsPerPerson) {
              foodPerPerson = (float) soc.getTotalFoodResource() / soc.getPopulation().size();
              matsPerPerson = (float) soc.getTotalRawMaterialResource()
                  / soc.getPopulation().size();
              bestTradingCandidate = soc;
            }
          }
        }
        if (bestTradingCandidate != null) {
          break;
        } else {
          society.incrementWrongMoves();
        }
      } else {
        // if move is 3 the society does nothing
        break;
      }
    }
    return move;
  }

  /**
   * The turn calculations needed for the Ai Societies to ake their turns.
   *
   * @param society the society
   */
  public static void aiTurn(Society society) {
    if (!society.hasMadeMove() && Game.getState() != GameState.DEALING) {
      // Get the turn ID;
      int turnId = getMoveID(society);
      if (turnId == 2) {
        // TRADING
        Game.getNotificationTimer().setDuration(Game.isTraining() ? 0 : 2);
        TradeDeal tradeDeal = calculateTradeDeal(society, bestTradingCandidate);
        if (tradeDeal.getSocietyB() == getActiveSocieties().get(0)
            && Game.getState() != GameState.GAME_PAUSE && !Game.isTraining()) {
          DealingMenu.setTradeDeal(tradeDeal);
          Game.setState(GameState.DEALING);
        } else {
          Game.setState(GameState.AI_DEALING);
          boolean accepted = tradeDeal.getSocietyB().examineTradeDeal(tradeDeal);
          if (accepted) {
            tradeDeal.setEndTurnOfDeal(Hud.getTurn()
                + TradingMenu.getDefaultLengthOfTradeDealInTurns());
            tradeDeal.getSocietyA().activateTradeDeal(tradeDeal);
            tradeDeal.getSocietyB().activateTradeDeal(tradeDeal);
            tradeDeal.getSocietyA().setEndTurn(true);
          }
          society.setMadeMove(true);
        }
      } else if (turnId == 1) {
        // WARRING
        Game.getNotificationTimer().setDuration(Game.isTraining() ? 0 : 2);
        // rank our possible tiles to attack with
        ArrayList<TileWorldObject> attackingTiles = society.getAttackingTiles();
        attackingTiles.sort((tile1, tile2)
            -> (Float.compare(tile2.getAttackingDesirability(),
            tile1.getAttackingDesirability())));
        // pick the best tile to attack with
        TileWorldObject attackingTile = attackingTiles.get(0);
        // Pick the best tile to attack
        society.calculateDefendingTiles(attackingTile);
        TileWorldObject defendingTile = null;
        for (TileWorldObject tile : society.getDefendingTiles()) {
          if (defendingTile == null || tile.getDefendingDesirability(society)
              > defendingTile.getDefendingDesirability(society)) {
            defendingTile = tile;
          }
        }
        assert defendingTile != null;
        targetSociety = defendingTile.getClaimedBy();
        simulateBattle(society, attackingTile, defendingTile);
        society.setMadeMove(true);
        Game.setState(GameState.AI_WAR);
      } else if (turnId == 0) {
        // CLAIMING
        Game.getNotificationTimer().setDuration(Game.isTraining() ? 0 : 2);
        TileWorldObject claimTile = calculateClaimTile(society);
        society.claimTile(claimTile);
        bordersAltered = true;
        updateSocietyBorders();
        society.setMadeMove(true);
        Game.setState(GameState.AI_CLAIM);
      } else {
        // NOTHING
        Game.getNotificationTimer().setDuration(Game.isTraining() ? 0 : 2);
        Game.setState(GameState.AI_NOTHING);
        society.setMadeMove(true);
      }
    }
    if (Game.getNotificationTimer().isDurationMet() && Game.getState() != GameState.DEALING) {
      Game.getNotificationTimer().clearDuration();
      society.setEndTurn(true);
      Game.setState(GameState.GAME_MAIN);
    }
  }

  private static TradeDeal calculateTradeDeal(Society currentPlayer, Society targetOfTradeDeal) {
    //initialise a new trade deal
    TradeDeal tradeDeal = new TradeDeal(0, 0, 0, 0);
    // add currentPlayer as Society A to the trade deal
    tradeDeal.setSocietyA(currentPlayer);
    // add targetOfTradeDeal as Society B of the Trade deal
    tradeDeal.setSocietyB(targetOfTradeDeal);
    // add to the trade deal the amount of food you need to full fill minimum quota
    while ((float) (currentPlayer.getTotalFoodResource() + tradeDeal.getFoodReceived())
        / currentPlayer.getPopulation().size() < Society.getFoodPerPerson()) {
      // check if the target of trade deal has enough food to give you what you require
      if (!(targetOfTradeDeal.getTotalFoodResource() - tradeDeal.getFoodReceived() <= 0)) {
        tradeDeal.setFoodReceived(tradeDeal.getFoodReceived() + 1);
      } else {
        break;
      }
    }
    // add to the trade deal the amount of food you need to full fill minimum quota
    while ((float) (currentPlayer.getTotalRawMaterialResource() + tradeDeal.getRawMatsReceived())
        / currentPlayer.getPopulation().size() < Society.getMaterialPerPerson()) {
      // check if the target of trade deal has enough food to give you what you require
      if (!(targetOfTradeDeal.getTotalRawMaterialResource()
          - tradeDeal.getRawMatsReceived() <= 0)) {
        tradeDeal.setRawMatsReceived(tradeDeal.getRawMatsReceived() + 1);
      } else {
        break;
      }
    }
    // add to trade deal any access amount of food the society has, assuming the minimum amount
    // of people are born come next reproduction
    int i = 0;
    while ((float) (currentPlayer.getTotalFoodResource() - tradeDeal.getFoodGiven())
        / ((float) currentPlayer.getPopulation().size() + Society.getOffspringAmount())
        > Society.getFoodPerPerson() && i < TradingMenu.getDefaultMaxValueForTradeDeals()) {
      tradeDeal.setFoodGiven(tradeDeal.getFoodGiven() + 1);
      i++;
    }
    // add to trade deal any access amount of Raw Mats the society has, assuming the minimum amount
    // of people are born come next reproduction
    int j = 0;
    while ((float) (currentPlayer.getTotalRawMaterialResource() - tradeDeal.getRawMatsGiven())
        / ((float) currentPlayer.getPopulation().size() + Society.getOffspringAmount())
        > Society.getFoodPerPerson() && j < TradingMenu.getDefaultMaxValueForTradeDeals()) {
      tradeDeal.setRawMarsGiven(tradeDeal.getRawMatsGiven() + 1);
      j++;
    }
    return tradeDeal;
  }

  private static TileWorldObject calculateClaimTile(Society society) {
    ArrayList<TileWorldObject> claimable = society.getClaimableTerritory();
    float[] scores = new float[claimable.size()];
    // Get the score for each tile
    for (int i = 0; i < claimable.size(); i++) {
      float score = calculateTileClaimScore(society, claimable.get(i));
      scores[i] = score;
    }
    // Get the highest score
    float highest = ArrayUtils.max(scores);
    int index = ArrayUtils.indexOf(scores, highest);
    return claimable.get(index);
  }

  private static float calculateTileClaimScore(Society society, TileWorldObject tile) {
    // Decides what tile the society would most want
    //Dictated by how much food they require, how much raw material they require;
    float foodWeight = (society.getPopulation().size() * Society.getFoodPerPerson())
        / (society.getTotalFoodResource() + 1);
    float materialWeight = (society.getPopulation().size() * Society.getMaterialPerPerson())
        / (society.getTotalRawMaterialResource() + 1);
    float tileAttackWeight = -(society.getAverageAge() / 100);

    return (foodWeight * tile.getFoodResource())
        + (materialWeight * tile.getRawMaterialResource())
        + (tileAttackWeight * tile.getTile().getAttackModifier());
  }

  public static ArrayList<Society> getActiveSocieties() {
    return activeSocieties;
  }
}
