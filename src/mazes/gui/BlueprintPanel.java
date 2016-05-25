/*******************************************************************************
 * Copyright (c) 2014-2016 Radon Rosborough. All rights reserved.
 *******************************************************************************/
package mazes.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.swing.AbstractAction;
import javax.swing.JApplet;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import mazes.schematic.Blueprint;
import mazes.schematic.XYZLine;

@SuppressWarnings("serial")
public final class BlueprintPanel extends JPanel {
	
	// [0] tile x -> screen x
	// [1] tile x -> screen y
	// [2] tile y -> screen x
	// [3] tile y -> screen y
	// [4] tile z -> screen x
	// [5] tile z -> screen y
	private int[] scaleFactors;
	private final int[] originalScaleFactors;
	// [0] minX [1] maxX [2] minY [3] maxY [4] minZ [5] maxZ
	private final int[] minMax;
	// {x, y, z}
	private final boolean[] reverseDirection;
	private final int windowBuffer;
	
	private final HashMap<String, List<Blueprint>> shapeLists; // tells us what to render
	private final HashMap<String, List<Boolean>> renderLists; // tells us whether or not to actually render each shape
	private final HashMap<String, Integer> indexList; // tells us which shape in a set is highlighted ("selected")
	private String currentKey; // tells us which set we're in, to index shapeLists and indexList
	private final int[] wasdPan = new int[2];
	
	private final ScheduledExecutorService resizeWindowTimer = Executors.newScheduledThreadPool(1);
	private ScheduledFuture<?> resizeWindowFuture;
	private final JApplet containingApplet;
	
	private final int resizeDelayMS;
	private boolean dimUnselected;
	
	public BlueprintPanel(HashMap<String, List<Blueprint>> shapeLists, int[] scaleFactors, boolean[] reverseDirection, int windowBuffer, JApplet containingApplet, int resizeDelayMS, boolean dimUnselected, String wasd) {
		super();
		
		this.containingApplet = containingApplet;
		this.resizeDelayMS = resizeDelayMS;
		this.dimUnselected = dimUnselected;
		setVisible(true);
		
		this.originalScaleFactors = Objects.requireNonNull(scaleFactors);
		this.scaleFactors = Arrays.copyOf(scaleFactors, 6);
		if (scaleFactors.length != 6) throw new IllegalArgumentException();
		this.reverseDirection = reverseDirection;
		this.windowBuffer = windowBuffer;
		
		this.shapeLists = shapeLists;
		this.renderLists = new HashMap<String, List<Boolean>>();
		this.indexList = new HashMap<String, Integer>();
		
		this.currentKey = null;
		for (String key : shapeLists.keySet()) {
			indexList.put(key, 0);
			renderLists.put(key, new ArrayList<Boolean>());
			for (int i=0; i<shapeLists.get(key).size(); i++) {
				renderLists.get(key).add(false);
			}
			
			// Keyboard actions to select a piece set
			getInputMap().put(KeyStroke.getKeyStroke(key), key);
			getActionMap().put(key, new AbstractAction() {
				@Override public void actionPerformed(ActionEvent ae) {
					currentKey = key;
					repaint();
				}
			});
		}
		
		// Keyboard action to deselect a piece set
		getInputMap().put(KeyStroke.getKeyStroke("BACK_QUOTE"), "BACK_QUOTE");
		getActionMap().put("BACK_QUOTE", new AbstractAction() {
			@Override public void actionPerformed(ActionEvent ae) {
				currentKey = null;
				repaint();
			}
		});
		
		// Keyboard actions to iterate through a piece set, highlighting ("selecting") pieces
		HashMap<String, Integer> arrowKeyToIndexOffset = new HashMap<>();
		arrowKeyToIndexOffset.put("SEMICOLON", -1);
		arrowKeyToIndexOffset.put("QUOTE", 1);
		for (Map.Entry<String, Integer> entry : arrowKeyToIndexOffset.entrySet()) {
			String key = entry.getKey();
			int offset = entry.getValue();
			// key is SEMICOLON or QUOTE, to trigger this action.
			// currentKey is the actual key (determined by the user) that selects a piece set.
			getInputMap().put(KeyStroke.getKeyStroke(key), key);
			getActionMap().put(key, new AbstractAction() {
				@Override public void actionPerformed(ActionEvent ae) {
					if (currentKey != null) {
						int a = indexList.get(currentKey) + offset;
						int q = shapeLists.get(currentKey).size();
						if (q == 0) return;
						indexList.put(currentKey, (a % q + q) % q);
						repaint();
					}
				}
			});
		}
		
		// Keyboard action to reset selection index to 0
		getInputMap().put(KeyStroke.getKeyStroke("0"), "0");
		getActionMap().put("0", new AbstractAction() {
			@Override public void actionPerformed(ActionEvent ae) {
				if (currentKey != null) {
					indexList.put(currentKey, 0);
				}
				repaint();
			}
		});
		
		// Keyboard actions to change "camera angle"
		{
			String[] keys = new String[] {"LEFT", "RIGHT", "UP", "DOWN"};
			for (int i=0; i<4; i++) {
				int j = i; // effectively final
				String key = keys[i];
				getInputMap().put(KeyStroke.getKeyStroke(key), key);
				getActionMap().put(key, new AbstractAction() {
					@Override public void actionPerformed(ActionEvent ae) {
						switch (j) {
						case 0: BlueprintPanel.this.scaleFactors[2] += 1; break;
						case 1: BlueprintPanel.this.scaleFactors[2] -= 1; break;
						case 2: BlueprintPanel.this.scaleFactors[3] += 1; break;
						case 3: BlueprintPanel.this.scaleFactors[3] -= 1; break;
						default: throw new AssertionError();
						}
						recalculateMinMax();
						repaint();
					}
				});
			}
		}
		
		// Keyboard actions to change scale factors directly
		{
			String[] keys = {"1", "shift 1", "2", "shift 2", "3", "shift 3", "4", "shift 4", "5", "shift 5", "6", "shift 6"};
			for (int i=0; i<12; i++) {
				String key = keys[i];
				final int index = i / 2;
				final int offset = i % 2 == 0 ? -1 : 1;
				getInputMap().put(KeyStroke.getKeyStroke(key), key);
				getActionMap().put(key, new AbstractAction() {
					@Override public void actionPerformed(ActionEvent ae) {
						BlueprintPanel.this.scaleFactors[index] += offset;
						recalculateMinMax();
						repaint();
					}
				});
			}
		}
		
		// Keyboard action to switch the visibility of a piece
		getInputMap().put(KeyStroke.getKeyStroke("SPACE"), "SPACE");
		getActionMap().put("SPACE", new AbstractAction() {
			@Override public void actionPerformed(ActionEvent ae) {
				if (currentKey != null) {
					renderLists.get(currentKey).set(indexList.get(currentKey), !renderLists.get(currentKey).get(indexList.get(currentKey)));
					repaint();
				}
			}
		});
		
		// Keyboard actions to switch the visibility of all pieces simultaneously
		HashMap<String, Boolean> braceKeyToRender = new HashMap<>();
		braceKeyToRender.put("MINUS", false);
		braceKeyToRender.put("EQUALS", true);
		for (Map.Entry<String, Boolean> entry : braceKeyToRender.entrySet()) {
			String key = entry.getKey();
			boolean render = entry.getValue();
			getInputMap().put(KeyStroke.getKeyStroke(key), key);
			getActionMap().put(key, new AbstractAction() {
				@Override public void actionPerformed(ActionEvent ae) {
					if (currentKey != null) {
						Collections.fill(renderLists.get(currentKey), render);
						repaint();
					}
				}
			});
		}
		
		// Keyboard action to completely reset piece visibility and selection indices
		getInputMap().put(KeyStroke.getKeyStroke("SLASH"), "SLASH");
		getActionMap().put("SLASH", new AbstractAction() {
			private int timesPressed = 0;
			private final ScheduledExecutorService cancelResetTimer = Executors.newScheduledThreadPool(1);
			private ScheduledFuture<?> cancelResetFuture = null;
			@Override public void actionPerformed(ActionEvent ae) {
				switch (timesPressed) {
				case 2:
					resetAll();
					break;
				case 1:
				case 0:
					timesPressed += 1;
					if (cancelResetFuture != null) {
						cancelResetFuture.cancel(false);
					}
					cancelResetFuture = cancelResetTimer.schedule(() -> {
						timesPressed = 0;
					}, 300, TimeUnit.MILLISECONDS);
					break;
				default:
					throw new AssertionError();
				}
			}
		});
		
		// Keyboard action to switch the dimming of unselected pieces
		getInputMap().put(KeyStroke.getKeyStroke("BACK_SLASH"), "BACK_SLASH");
		getActionMap().put("BACK_SLASH", new AbstractAction() {
			@Override public void actionPerformed(ActionEvent ae) {
				BlueprintPanel.this.dimUnselected = !BlueprintPanel.this.dimUnselected;
				repaint();
			}
		});
		
		if (wasd != null) {
			// Keyboard actions to pan the camera orthogonally
			getInputMap().put(KeyStroke.getKeyStroke(wasd.substring(0, 1)), wasd.substring(0, 1));
			getActionMap().put(wasd.substring(0, 1), new AbstractAction() {
				@Override public void actionPerformed(ActionEvent ae) {
					wasdPan[1] -= 20;
					repaint();
				}
			});
			getInputMap().put(KeyStroke.getKeyStroke(wasd.substring(1, 2)), wasd.substring(1, 2));
			getActionMap().put(wasd.substring(1, 2), new AbstractAction() {
				@Override public void actionPerformed(ActionEvent ae) {
					wasdPan[0] -= 20;
					repaint();
				}
			});
			getInputMap().put(KeyStroke.getKeyStroke(wasd.substring(2, 3)), wasd.substring(2, 3));
			getActionMap().put(wasd.substring(2, 3), new AbstractAction() {
				@Override public void actionPerformed(ActionEvent ae) {
					wasdPan[1] += 20;
					repaint();
				}
			});
			getInputMap().put(KeyStroke.getKeyStroke(wasd.substring(3, 4)), wasd.substring(3, 4));
			getActionMap().put(wasd.substring(3, 4), new AbstractAction() {
				@Override public void actionPerformed(ActionEvent ae) {
					wasdPan[0] += 20;
					repaint();
				}
			});
		}
		
		this.minMax = new int[4];
		recalculateMinMax(); // also resized window (instantly the first time)
		
		System.out.println("Done.");
		repaint();
	}
	
	private void recalculateMinMax() {
		int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE,
				minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE,
				minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
		
		for (List<Blueprint> shapeList : shapeLists.values()) {
			for (Blueprint shape : shapeList) {
				for (XYZLine line : shape) {
					// since we are directly calculating the actual minimum and maximum screen coordinates,
					// and scaling all other rendering based on that, it doesn't matter how we scale the
					// coordinates (even if we make them negative!); all will be rendered properly.
					minX = Math.min(minX, Math.min(line.x1 * (reverseDirection[0] ? -1 : 1), line.x2 * (reverseDirection[0] ? -1 : 1)));
					maxX = Math.max(maxX, Math.max(line.x1 * (reverseDirection[0] ? -1 : 1), line.x2 * (reverseDirection[0] ? -1 : 1)));
					minY = Math.min(minY, Math.min(line.y1 * (reverseDirection[1] ? -1 : 1), line.y2 * (reverseDirection[1] ? -1 : 1)));
					maxY = Math.max(maxY, Math.max(line.y1 * (reverseDirection[1] ? -1 : 1), line.y2 * (reverseDirection[1] ? -1 : 1)));
					minZ = Math.min(minZ, Math.min(line.z1 * (reverseDirection[2] ? -1 : 1), line.z2 * (reverseDirection[2] ? -1 : 1)));
					maxZ = Math.max(maxZ, Math.max(line.z1 * (reverseDirection[2] ? -1 : 1), line.z2 * (reverseDirection[2] ? -1 : 1)));
				}
			}
		}
//		System.out.println("Space coordinates: " + Arrays.toString(new int[] {minX, maxX, minY, maxY, minZ, maxZ}));
		
		// stands for MINimum Screen X coordinate, and so on...
		int minSX = Integer.MAX_VALUE, maxSX = Integer.MIN_VALUE,
				minSY = Integer.MAX_VALUE, maxSY = Integer.MIN_VALUE;
		for (int x : new int[] {minX, maxX}) {
			for (int y : new int[] {minY, maxY}) {
				for (int z : new int[] {minZ, maxZ}) {
					int sx = x * scaleFactors[0] + y * scaleFactors[2] + z * scaleFactors[4];
					int sy = x * scaleFactors[1] + y * scaleFactors[3] + z * scaleFactors[5];
					minSX = Math.min(minSX, sx); maxSX = Math.max(maxSX, sx);
					minSY = Math.min(minSY, sy); maxSY = Math.max(maxSY, sy);
				}
			}
		}
		
//		this.minMax = new int[] {minSX, maxSX, minSY, maxSY}; // can't assign directly - oops, I guess final can be a little annoying.
		minMax[0] = minSX; minMax[1] = maxSX; minMax[2] = minSY; minMax[3] = maxSY;
		
//		System.out.println("Screen coordinates: " + Arrays.toString(minMax));
		
		if (resizeDelayMS == -1) resizeWindow();
		else {
			if (resizeWindowFuture != null) {
				resizeWindowFuture.cancel(false);
			}
			resizeWindowFuture = resizeWindowTimer.schedule(this::resizeWindow, resizeDelayMS, TimeUnit.MILLISECONDS);
		}
	}
	private void resizeWindow() {
		setPreferredSize(new Dimension(
				(minMax[1] - minMax[0]) + windowBuffer * 2 + 1,
				(minMax[3] - minMax[2]) + windowBuffer * 2 + 1
				));
		setSize(getPreferredSize());
		containingApplet.setPreferredSize(getPreferredSize());
		containingApplet.setSize(getPreferredSize());
		repaint();
	}
	private void resetAll() {
		currentKey = null;
		for (String key : shapeLists.keySet()) {
			indexList.put(key, 0);
			Collections.fill(renderLists.get(key), false);
		}
		scaleFactors = Arrays.copyOf(originalScaleFactors, originalScaleFactors.length);
		recalculateMinMax(); // then resizeWindow() then repaint()
		wasdPan[0] = 0;
		wasdPan[1] = 0;
	}
	
	@Override public void paintComponent(Graphics G) {
		Graphics2D g = (Graphics2D) G;
//		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, getWidth(), getHeight());
		
		Blueprint selectedShape = null;
		for (String key : shapeLists.keySet()) {
			
			List<Blueprint> shapes = shapeLists.get(key);
			List<Boolean> renderList = renderLists.get(key);
			int selectedIndex = indexList.get(key);
			
			// For each shape, if it is visible...
			for (int i=0; i<shapes.size(); i++) {
				boolean isSelected = key.equals(currentKey) && i == selectedIndex;
				Blueprint shape = shapes.get(i);
				if (isSelected) {
					// (unless it is selected)
					selectedShape = shape;
				}
				else if (renderList.get(i)) {
					// ... then render it.
					drawShape(shape, false, g);
				}
			}
		}
		if (selectedShape != null) {
			drawShape(selectedShape, true, g);
		}
	}
	
	private void drawShape(Blueprint shape, boolean isSelected, Graphics g) {
		for (XYZLine line : shape) {
			Color color = isSelected || !dimUnselected ? line.colors.active : line.colors.inactive;
			if (color == null) continue; // if color is null, don't render
			else g.setColor(color);
			
//			System.out.println("Drawing line " + line + " with " + g.getColor());
			
			// standing for Original Screen X-coordinate #1, and so on.
			int osx1 = line.x1 * scaleFactors[0] + line.y1 * scaleFactors[2] + line.z1 * scaleFactors[4],
					osy1 = line.x1 * scaleFactors[1] + line.y1 * scaleFactors[3] + line.z1 * scaleFactors[5];
			int osx2 = line.x2 * scaleFactors[0] + line.y2 * scaleFactors[2] + line.z2 * scaleFactors[4],
					osy2 = line.x2 * scaleFactors[1] + line.y2 * scaleFactors[3] + line.z2 * scaleFactors[5];
			// the initial range is guaranteed because we actually calculated minSX and maxSX
			// by computing every value in the range and finding the maximum and minimum
			// (this range)   v-------------v
			// transform from [minX ... maxX] to reverseDirection[0] ? [maxSX - minSX ... 0] : [0 ... maxSX - minSX]
			int sx1 = osx1 - minMax[0], sx2 = osx2 - minMax[0];
			int sy1 = osy1 - minMax[2], sy2 = osy2 - minMax[2]; // minMax = {minSX, maxSX, minSY, maxSY}
//			System.out.println(Arrays.toString(new int[] {osx1, osy1}) + " |> " +
//					Arrays.toString(new int[] {sx1, sy1}) + " given " + Arrays.toString(minMax));
			if (reverseDirection[0]) {
				sx1 = (minMax[1] - minMax[0]) /* (maxSX - minSX) */ - sx1;
				sx2 = (minMax[1] - minMax[0]) - sx2;
			}
			if (reverseDirection[1]) {
				sy1 = (minMax[3] - minMax[2]) - sy1;
				sy2 = (minMax[3] - minMax[2]) - sy2;
			}
//			System.out.println("Rendering " + Arrays.toString(new int[] {sx1, sy1, sx2, sy2}) + " given " + Arrays.toString(minMax));
			g.drawLine(
					windowBuffer + sx1 + wasdPan[0],
					windowBuffer + sy1 + wasdPan[1],
					windowBuffer + sx2 + wasdPan[0],
					windowBuffer + sy2 + wasdPan[1]
					);
		}
	}
	
}
