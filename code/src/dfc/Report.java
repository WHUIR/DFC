package dfc;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by csq
 */
public class Report {

	protected static void accReport(SModel model) {
		int correct = 0;
		int total = 0;
		int[] resT = new int[model.cateNum];
		int[] resS = new int[model.cateNum];

		for (int i = 0; i < model.docNum; i++) {
			if (model.documents[i].check) {
				total++;
				if (model.documents[i].groundTruth == model.documents[i].prediction) {
					correct++;
					resT[model.documents[i].prediction]++;
				}
				resS[model.documents[i].prediction]++;
			}
		}

		for (int i = 0; i < model.docNum; i++) {
			if (model.documents[i].check) {
				System.out.println("prediction: "
						+ model.documents[i].prediction);
				System.out.println("groundtruth: "
						+ model.documents[i].groundTruth);
			}
		}

		double re = (double) correct / total;
		System.out.println("accuracy: " + re);
	}

	protected static void macroReport(SModel model, int iter)
			throws IOException {

		int[] resT = new int[model.cateNum];
		int[] resS = new int[model.cateNum];
		for (int i = 0; i < model.docNum; i++) {
			if (model.documents[i].check) {
				if (model.documents[i].groundTruth == model.documents[i].prediction) {
					resT[model.documents[i].prediction]++;
				}
				resS[model.documents[i].prediction]++;
			}
		}
		double p, r, macroF1 = 0;
		model.resultWriter.write("bcatenum:" + model.bCateNum + "\r\n");
		model.resultWriter.write("alpha2:" + model.alpha2 + "\r\n");
		model.resultWriter.write("topicnum:" + model.topicNum + "\r\n");
		model.resultWriter.write("rho:" + model.rho + "\r\n");
		model.resultWriter.write("fakeSeedNum:" + model.fakeSeedNum + "\r\n");
		model.resultWriter.write(iter + "\r\n");
		for (int i = 0; i < model.iCateNum; i++) {
			System.out.println(i + ": " + resT[i] + " " + resS[i] + " "
					+ model.groundTruth[i]);
			model.resultWriter.write(i + "  resT:" + resT[i] + " resP:"
					+ resS[i] + " resG:" + model.groundTruth[i] + "\r\n");
			p = (double) resT[i] / resS[i];
			r = (double) resT[i] / model.groundTruth[i];
			if (p == 0 || r == 0)
				continue;
			macroF1 += 2 * p * r / (p + r);
		}
		double re = macroF1 / model.iCateNum;
		System.out.println("f1: " + re);
		model.resultWriter.write("f1: " + re + "\r\n");
	}

}
