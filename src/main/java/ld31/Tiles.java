package ld31;

class Tiles {
	final static int tiles_pacmanclassic_W = 28;
	final static int tiles_pacmanclassic_H = 31;
	final static int[] tiles_pacmanclassic = {
		00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,
		00,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	00,	00,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	00,
		00,	07,	00,	00,	00,	00,	07,	00,	00,	00,	00,	00,	07,	00,	00,	07,	00,	00,	00,	00,	00,	07,	00,	00,	00,	00,	07,	00,
		00,	11,	00,	00,	00,	00,	07,	00,	00,	00,	00,	00,	07,	00,	00,	07,	00,	00,	00,	00,	00,	07,	00,	00,	00,	00,	11,	00,
		00,	07,	00,	00,	00,	00,	07,	00,	00,	00,	00,	00,	07,	00,	00,	07,	00,	00,	00,	00,	00,	07,	00,	00,	00,	00,	07,	00,
		00,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	00,
		00,	07,	00,	00,	00,	00,	07,	00,	00,	07,	00,	00,	00,	00,	00,	00,	00,	00,	07,	00,	00,	07,	00,	00,	00,	00,	07,	00,
		00,	07,	00,	00,	00,	00,	07,	00,	00,	07,	00,	00,	00,	00,	00,	00,	00,	00,	07,	00,	00,	07,	00,	00,	00,	00,	07,	00,
		00,	07,	07,	07,	07,	07,	07,	00,	00,	07,	07,	07,	07,	00,	00,	07,	07,	07,	07,	00,	00,	07,	07,	07,	07,	07,	07,	00,
		00,	00,	00,	00,	00,	00,	07,	00,	00,	00,	00,	00,	07,	00,	00,	07,	00,	00,	00,	00,	00,	07,	00,	00,	00,	00,	00,	00,
		00,	00,	00,	00,	00,	00,	07,	00,	00,	00,	00,	00,	07,	00,	00,	07,	00,	00,	00,	00,	00,	07,	00,	00,	00,	00,	00,	00,
		00,	00,	00,	00,	00,	00,	07,	00,	00,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	00,	00,	07,	00,	00,	00,	00,	00,	00,
		00,	00,	00,	00,	00,	00,	07,	00,	00,	07,	00,	00,	00,	01,	01,	00,	00,	00,	07,	00,	00,	07,	00,	00,	00,	00,	00,	00,
		00,	00,	00,	00,	00,	00,	07,	00,	00,	07,	00,	01,	01,	01,	01,	01,	01,	00,	07,	00,	00,	07,	00,	00,	00,	00,	00,	00,
		07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	00,	01,	01,	01,	01,	01,	01,	00,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,
		00,	00,	00,	00,	00,	00,	07,	00,	00,	07,	00,	01,	01,	01,	01,	01,	01,	00,	07,	00,	00,	07,	00,	00,	00,	00,	00,	00,
		00,	00,	00,	00,	00,	00,	07,	00,	00,	07,	00,	00,	00,	00,	00,	00,	00,	00,	07,	00,	00,	07,	00,	00,	00,	00,	00,	00,
		00,	00,	00,	00,	00,	00,	07,	00,	00,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	00,	00,	07,	00,	00,	00,	00,	00,	00,
		00,	00,	00,	00,	00,	00,	07,	00,	00,	07,	00,	00,	00,	00,	00,	00,	00,	00,	07,	00,	00,	07,	00,	00,	00,	00,	00,	00,
		00,	00,	00,	00,	00,	00,	07,	00,	00,	07,	00,	00,	00,	00,	00,	00,	00,	00,	07,	00,	00,	07,	00,	00,	00,	00,	00,	00,
		00,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	00,	00,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	00,
		00,	07,	00,	00,	00,	00,	07,	00,	00,	00,	00,	00,	07,	00,	00,	07,	00,	00,	00,	00,	00,	07,	00,	00,	00,	00,	07,	00,
		00,	07,	00,	00,	00,	00,	07,	00,	00,	00,	00,	00,	07,	00,	00,	07,	00,	00,	00,	00,	00,	07,	00,	00,	00,	00,	07,	00,
		00,	11,	07,	07,	00,	00,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	00,	00,	07,	07,	11,	00,
		00,	00,	00,	07,	00,	00,	07,	00,	00,	07,	00,	00,	00,	00,	00,	00,	00,	00,	07,	00,	00,	07,	00,	00,	07,	00,	00,	00,
		00,	00,	00,	07,	00,	00,	07,	00,	00,	07,	00,	00,	00,	00,	00,	00,	00,	00,	07,	00,	00,	07,	00,	00,	07,	00,	00,	00,
		00,	07,	07,	07,	07,	07,	07,	00,	00,	07,	07,	07,	07,	00,	00,	07,	07,	07,	07,	00,	00,	07,	07,	07,	07,	07,	07,	00,
		00,	07,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	07,	00,	00,	07,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	07,	00,
		00,	07,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	07,	00,	00,	07,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	07,	00,
		00,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	07,	00,
		00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,	00,
	};

	public final static int EMPTY			= 0x00;
	public final static int GHOST_ALLOWED	= 0x01;
	public final static int PLAYER_ALLOWED	= 0x02;
	public final static int PELLET			= 0x04;
	public final static int BOOST			= 0x08;

	public final int width = tiles_pacmanclassic_W;
	public final int height = tiles_pacmanclassic_H;
	private final int[] tiles = new int[width * height];

	public Tiles() {
		reset();
	}

	public void reset() {
		System.arraycopy(tiles_pacmanclassic, 0, tiles, 0, tiles.length);
	}

	public int tile(int x, int y) {
		return tiles[x + y * width];
	}

	public boolean has(int mask, int x, int y) {
		int v = (x < 0 || x >= width || y < 0 || y >= height)? PLAYER_ALLOWED : tiles[x + y * width];
		return (v == mask) || ((v & mask) != 0); // first test to match EMPTY mask
	}
}
