Check out the video [here](https://youtu.be/4UkSArJKgSg).

# General info

In the fall of my junior year in high school, I took a Design
Technology class. For my final project, I constructed a
three-dimensional marble maze using the laser cutter. It would have
been intractably difficult to design the maze by hand, so I wrote this
library to generate the schematics. You can see a making-of video at
my website [here][other projects].

The endpoints of the library are as follows:
- `mazes.gui.MazeApplet2D` generates and displays two-dimensional
  mazes
- `mazes.gui.MazeApplet3D` generates and displays three-dimensional
  mazes
- `mazes.gui.MazeApplet4D` generates and displays four-dimensional
  mazes
- `mazes.schematic.SchematicGenerator` allows bulk generation of 3D
  mazes to find ones that can be built
- `mazes.gui.BlueprintApplet` shows the pieces in simplified form, and
  shows pieces with poor support
- `mazes.gui.SchematicApplet` shows the pieces in full detail
- `mazes.schematic.SchematicExporter` exports SVG files containing the
  pieces for the laser cutter

This is pretty old code. Nowadays I like to put a docstring on every
method, but back then... yeah, it's not pretty. And can you guess who
didn't use source control? That's right. Sometime in the far-off
future I might return to this project and redo it, but probably not
anytime soon. What is more likely is my updating this README file to
include more helpful information; currently it is quite spare, as you
can see. If you are interested in this project, please feel free to
contact me at [radon.rosborough@gmail.com][email] and I would be happy
to answer any questions you might have.

## Miscellany

- I recommend running this code from Eclipse. This will allow you to
  easily run the library from each of the entry points listed above.
  Plus, some of the applets require console interaction.
- It's currently unclear to me (this project having been written 2+
  years ago) whether the mazes or the bin/mazes directory is used for
  saving and loading maze files. Or perhaps both are used.
- It takes a long time (possibly as much as several days) to generate
  a valid `6x6x6` maze using SchematicGenerator. `5x5x5` is much more
  reasonable and any smaller size is super fast.
- The schematic for the maze I actually built is located in
  `mazes-svg/maze6_000`, and the maze file is `mazes/maze6_000.chmz`.
  I'm unsure why there's also a file named `mazes/maze6_000.maze`. The
  `mazes-svg/maze6_000_classic` folder is kept for only sentimental
  reasons.
- `.maze` files are unchecked mazes and `.chmz` files are checked
  mazes. Checked mazes don't have unsupported pieces and other
  features that would make them impossible to build. It still took 24+
  hours for my Dad and I to construct the real maze, though.
- The total time for this project was 4 weeks for coding and 2 weeks
  for construction, at approximately 60 hours/week.
- Various dimensions (such as the thickness of material being used for
  the laser cutter) can be changed in `mazes.schematic.Dimensions`.
- The algorithm used in this project to generate mazes is called
  the [Growing Tree algorithm][growing tree algorithm]. I think I used
  a 50/50 split between Newest/Random (see the linked website for an
  explanation). It's easily generalized to an arbitrary number of
  dimensions, and it is used without modification in each of the three
  versions of `MazeApplet`.

# Applets

## MazeApplet2D

### Configuration

    MazeApplet2D.windowSize
    MazeApplet2D.mazeSize

### Keyboard controls

- `␣` to advance maze generation
- `A` to toggle animation of maze generation

## MazeApplet3D

Sometimes the window does not resize automatically to fit the maze. If
this is the case, resize it manually.

### Configuration

    MazeApplet3D.gridSize
    MazeApplet3D.windowBuffer
    MazeApplet3D.mazeSize
    MazeApplet3D.lineWidth

### Keyboard controls

- `␣` to advance maze generation
- `A` to toggle animation of maze generation
- `F` to complete maze generation
- `R` to reset maze generation
- `S` to save current maze generator status to a file (requires
  console interaction)
- `L` to load a previously saved file (requires console interaction)
- `← → ↑ ↓` to change the camera angle
- `O` to switch the orientation of the maze
- `Q` to generate a new random maze
- `C` to toggle display of solution

## MazeApplet4D

Sometimes the window does not resize automatically to fit the maze. If
this is the case, resize it manually.

### Configuration

    MazeApplet4D.gridSize
    MazeApplet4D.layerOffsetX
    MazeApplet4D.layerOffsetY
    MazeApplet4D.metalayerOffsetX
    MazeApplet4D.metalayerOffsetY
    MazeApplet4D.windowBuffer
    MazeApplet4D.mazeSize
    MazeApplet4D.lineWidth

### Keyboard controls

- `␣` to advance maze generation
- `A` to toggle animation of maze generation

## BlueprintApplet and SchematicApplet

You will need to enter the name of a maze file at the command line on
the launch of both of these applets. This can be a maze that has been
completed generated by MazeApplet3D (press S in that applet to save),
or it can be one generated by SchematicGenerator.

### Keyboard controls

#### For BlueprintApplet

- `T L O P E K C` to select a piece set (stands for [T]etris pieces,
  [L]ayer pieces, [O]utlines of layer pieces, [P]erforations in layer
  pieces, perforations in layer pieces with [E]rrors displayed (i.e.
  unsupported pieces), [K]omposite layer pieces, [C]omposite tetris
  pieces)

#### For SchematicApplet

- `R T Y` to select the tetris piece set
- `J K L` to select the layer piece set
- `A S D` to select the side piece set

#### For both

- `` ` `` to deselect the current piece set
- `; '` to go backwards and forwards through the currently selected
  piece set
- `0` to reset the selected piece to the first (default) piece (after
  it has been changed by semicolon and quote)
- `← → ↑ ↓` to change the camera angle
- `G V B N` to pan the camera orthogonally
- `1 ... 6` `shift-1 ... shift-6` to modify scale factors (advanced)
- `␣` to toggle piece visibility - if a piece is "visible" you can see
  it even when other pieces are selected (multiple pieces may be
  "visible", even if they are in different piece sets)
- `- =` to make "not visible" and "visible" all the pieces in the
  currently selected set, respectively
- `\` to swap whether "visible" but non-selected pieces are dimmed
  relative to the selected piece
- `/` three times quickly to reset all selections and visibility as
  well as the camera angle

## SchematicGenerator and SchematicExporter

These require console interaction and have no graphical interface.

[email]: mailto:radon.rosborough@gmail.com
[growing tree algorithm]: http://weblog.jamisbuck.org/2011/1/27/maze-generation-growing-tree-algorithm
[other projects]: https://intuitiveexplanations.com/other-projects/
