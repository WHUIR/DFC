package dfc;

import Util.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by csq
 */
public class SDocument {
	protected int prediction;
	protected int groundTruth;
	protected boolean check;
	protected String title;
	protected List<Pair> contents;
	protected List<Integer> xvec;
	protected int index;
	protected double[] scores;
	protected int doclength;

	protected int y;

	protected SDocument(int prediction, List<Pair> contents) {
		this.prediction = prediction;
		this.contents = contents;
	}

	protected SDocument() {
		contents = new ArrayList<>();
		xvec = new ArrayList<>();
	}
}
