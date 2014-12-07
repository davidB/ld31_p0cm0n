package ld31;

import javafx.scene.Node;
import javafx.scene.Parent;


public class Helpers4Javafx {
	public static Node findFocused(Node e) {
		if (e instanceof Parent) {
			Parent parent = (Parent) e;
			for (Node node : parent.getChildrenUnmodifiable()) {
				node = findFocused(node);
				if (node != null)
					return node;
			}
		}
		if (e.isFocusTraversable()) {
			if (e.isFocused()) {
				return e;
			}
		}
		return null;
	}
}
