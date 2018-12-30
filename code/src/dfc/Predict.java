package dfc;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import Util.Dictionary;
import Util.MathUtil;

/**
 * Created by csq
 */
public class Predict implements Decorator {

	private SModel model;
	double[] scores;

	public Predict(SModel model) {
		this.model = model;
	}

	private void predict() throws IOException {

		int[][] xmirror = new int[model.cateNum][model.maxDocLen];
		int[][] zmirror = new int[model.cateNum][model.maxDocLen];
		int[][] wmirror = new int[model.cateNum][model.maxDocLen];

		scores = new double[model.docNum];
		// each iter
		for (int j = 0; j < model.inter; j++) {
			System.out.println("iter: " + j);
			// evaluation result
			if (model.report == 1) {
				Report.macroReport(model, j);
			} else if (model.report == 0)
				Report.accReport(model);

			long startTime = System.currentTimeMillis();
			// each doc
			for (int i = 0; i < model.docNum; i++) {
				SDocument doc = model.documents[i];
				if (doc.contents.size() == 0)
					continue;
				// recount phi
				for (int k = 0; k < model.topicNum; k++) {
					model.categoryTopic[doc.prediction][k] -= model.docTopic[i][k];
					model.numRWords4Cate[doc.prediction] -= model.docTopic[i][k];
				}

				model.phiSum[doc.prediction] = 0;
				for (int k = 0; k < model.topicNum; k++) {
					model.phi[doc.prediction][k] = (model.categoryTopic[doc.prediction][k] + model.alpha0)
							/ (model.numRWords4Cate[doc.prediction] + model.topicNum
									* model.alpha0);
					model.phi[doc.prediction][k] *= model.alpha2;
					model.phiSum[doc.prediction] += model.phi[doc.prediction][k];
				}

				int res;
				// specifed categories
				for (int c = 0; c < model.iCateNum; c++) {
					// copy the mirror image of document
					copy(xmirror[c], zmirror[c], wmirror[c], doc);

					// sampling the topic distribution of each word
					Sampling.sampleXZ(i, model, c, xmirror, zmirror);

					// compute the probability of document
					doc.scores[c] = Sampling.sampleC(i, model, c, xmirror[c],
							zmirror[c], wmirror[c]);

					// rollback
					Sampling.rollBackForC(i, model, c, xmirror[c], zmirror[c]);
				}

				// irrelevant categories
				for (int c = model.iCateNum; c < model.cateNum; c++) {
					// copy the mirror image of document
					copy(xmirror[c], zmirror[c], wmirror[c], doc);

					// sampling the topic distribution of each word
					Sampling.sampleXZ(i, model, c, xmirror, zmirror);

					// compute the probability of document
					doc.scores[c] = Sampling.sampleB(i, model, c, xmirror[c],
							zmirror[c], wmirror[c]);

					// roll back
					Sampling.rollBackForB(i, model, c, xmirror[c], zmirror[c]);
				}

				// sampling the topic distribution of the document
				res = MathUtil.sample_neg(doc.scores);

				// update the topic of the document and each word in it
				Sampling.update(i, model, xmirror[res], zmirror[res], res);

				// recalculate phi
				for (int k = 0; k < model.topicNum; k++) {
					model.categoryTopic[doc.prediction][k] += model.docTopic[i][k];
					model.numRWords4Cate[doc.prediction] += model.docTopic[i][k];
				}
				model.phiSum[doc.prediction] = 0;
				for (int k = 0; k < model.topicNum; k++) {
					model.phi[doc.prediction][k] = (model.categoryTopic[doc.prediction][k] + model.alpha0)
							/ (model.numRWords4Cate[doc.prediction] + model.topicNum
									* model.alpha0);
					model.phi[doc.prediction][k] *= model.alpha2;
					model.phiSum[doc.prediction] += model.phi[doc.prediction][k];
				}

			}
			long endTime = System.currentTimeMillis();
			System.out.println("cost time " + (endTime - startTime) + "ms");

		}
		model.resultWriter.close();
		System.out
				.println("--------------------End output the result--------------------");
	}

	protected static void copy(int[] xmirror, int[] zmirror, int[] wmirror,
			SDocument doc) {
		for (int w = 0; w < doc.contents.size(); w++) {
			xmirror[w] = doc.xvec.get(w);
			zmirror[w] = doc.contents.get(w).topic;
			wmirror[w] = doc.contents.get(w).word;
		}
	}

	/*
	 * output the categoey precition
	 */
	public void out_precition() {
		int[][] pre_grd = new int[model.ctruth][model.cateNum];
		for (int i = 0; i < model.docNum; i++) {
			if (model.documents[i].check) {
				pre_grd[model.documents[i].groundTruth][model.documents[i].prediction]++;
			}
		}
		for (int i = 0; i < model.ctruth; i++) {
			System.out.println("groundTruth " + i + " :");
			for (int j = 0; j < model.cateNum; j++) {
				System.out.print(j + ":" + pre_grd[i][j] + " ");
			}
			System.out.println();
		}
	}

	@Override
	public SModel decorateSModel() {
		System.out.println("start to predict...");
		try {
			predict();
		} catch (IOException e) {
			e.printStackTrace();
		}
		out_precition();
		return model;
	}
}
