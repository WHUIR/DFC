package dfc;

import Util.Dictionary;
import Util.LuceneHandler;
import Util.MathUtil;
import Util.Pair;

import org.apache.lucene.document.Document;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by XJ on 2016/3/10. Update by Csq on 2017/4/7 Update the
 * function:load(),initCont()
 */
public class LoadDocs implements Decorator {

	private SModel model;
	private float soomth;
	public int seedwordnum;

	public LoadDocs(SModel model) {
		this.model = model;
		// soomth = (float) 1 / model.cateNum;
		soomth = (float) 1 / model.iCateNum;
		seedwordnum = 0;
	}

	/*
	 * 判断文档的相关性，初始化每篇有关文档 param (文档内容，文档对象)
	 */
	private void initContForFilter(String cont, SDocument sDoc) {
		String[] ves = cont.split(" ");

		// 当前文档包含的interesting category的seed word个数 f(d,c),用于计算ηd
		double[] re = new double[model.iCateNum];
		// int count=0;
		for (String s : ves) {
			// 只要包括一个类别的seed word就把该文档初始化为有关文档
			for (int cate = 0; cate < model.iCateNum; cate++)
				if (model.seedSet[cate].contains(s)) {
					sDoc.y = 1;
					// count++;
					// seedwordnum++;
					re[cate]++;
				}
			/*
			 * if(count>4) sDoc.y=1;
			 */
		}
		// 计算该篇文档的ηd(c)，c∈interesting category
		initEta(re, sDoc.index);

		// 伪相关文档
		if (sDoc.y == 1) {
			int cate = 0;
			// double maxEta = model.eta[sDoc.index][0];
			// 根据η进行类别的采样
			// cate = MathUtil.sample_neg(model.eta[sDoc.index]);
			cate = MathUtil.sample(model.eta[sDoc.index]);
			// 得到当前文档的初始化类别，取η最大值
			/*
			 * for(int i=1;i<model.iCateNum;i++){
			 * if(model.eta[sDoc.index][i]>maxEta){
			 * maxEta=model.eta[sDoc.index][i]; cate=i; } }
			 */
			sDoc.prediction = cate;
			int word, topic;
			for (String s : ves) {
				word = Dictionary.contains(s);
				if (word == -1)
					continue;
				if (model.seedSet[sDoc.prediction].contains(Dictionary
						.getWord(word))
						&& Math.random() < 0.5
						|| Math.random() < (model.rho)) {
					// if
					// (model.seedSet[sDoc.prediction].contains(Dictionary.getWord(word))&&
					// Math.random() < 0.5|| Math.random() < 0.5) {
					// if(Math.random()<model.tao[word][sDoc.prediction]){
					sDoc.xvec.add(0);
					// docX[0]统计的是当前文档的category word个数 docX[1]统计非category word数
					model.docX[sDoc.index][0]++;
					// x=0的情况下，每个词的topic都初始化为-1
					sDoc.contents.add(new Pair(word, -1));
					// Ncw++,Ncw word被分到category c的次数
					model.categoryWord[sDoc.prediction][word]++;
					// 当前文档的category Cd下的category word数 ∑Ncw
					model.numCWords4Cate[sDoc.prediction]++;
				}
				// 不是category word
				else {
					model.docX[sDoc.index][1]++;
					sDoc.xvec.add(1);
					// 随机为x=1的word 分配一个general-topic
					topic = (int) (Math.random() * model.topicNum);
					sDoc.contents.add(new Pair(word, topic));
					// Nct++，Nct： word在cate c下的文档内且被分到topic t下
					model.categoryTopic[sDoc.prediction][topic]++;
					// Ndt++,Ndt: 文档d中的词被分到topic t下的次数
					model.docTopic[sDoc.index][topic]++;
					// Ntw++,Ntw: word w被分到general-topic t下
					model.wordTopic[word][topic]++;
					// ∑Nct,属于文档类别是c的文档的所有词 且 x=1，不是category word (the total
					// number of words within the doc of category c and the
					// words are not category words)
					model.numRWords4Cate[sDoc.prediction]++;
					// ∑Ntw,分到topic t下的总词数
					model.numRWords4Topic[topic]++;
				}

			}
			sDoc.doclength = sDoc.contents.size();
		}// end sDoc.y==1

	}

	// classification
	private void load() {
		LuceneHandler lh = new LuceneHandler(model.luceneIndexPath);
		Document[] lucDocs = lh.getDocs();

		int len = model.docNum;
		if (len == 0) {
			System.out.println("there is no document in the path");
			System.exit(-1);
		}
		SDocument sDoc;
		Document luceneDoc;

		for (int i = 0; i < len; i++) {
			model.documents[i] = new SDocument();
			luceneDoc = lucDocs[i];
			sDoc = model.documents[i];

			sDoc.index = i;
			model.kappa[i] = model.kd;
			sDoc.y = 1;
			sDoc.scores = new double[model.cateNum];
			sDoc.title = luceneDoc.get(LuceneHandler.TITLE);
			sDoc.groundTruth = Integer.parseInt(luceneDoc
					.get(LuceneHandler.CATE));
			sDoc.check = Integer.parseInt(luceneDoc.get(LuceneHandler.CHECK)) == 1 ? true
					: false;

			sDoc.xvec = new ArrayList<>();
			if (sDoc.check)
				model.groundTruth[sDoc.groundTruth]++;
			initCont(luceneDoc.get(LuceneHandler.ABSTRACT), sDoc);
		}
	}

	// classification
	private void initCont(String cont, SDocument sDoc) {

		String[] ves = cont.split(" ");
		double[] re = new double[model.cateNum];

		for (String s : ves)
			for (int cate = 0; cate < model.cateNum; cate++)
				if (model.seedSet[cate].contains(s))
					re[cate]++;
		initEta(re, sDoc.index);
		sDoc.prediction = MathUtil.sample(model.eta[sDoc.index]);
		int word, topic;

		for (String s : ves) {
			word = Dictionary.contains(s);
			if (word == -1)
				continue;
			// category word
			if (model.seedSet[sDoc.prediction].contains(Dictionary
					.getWord(word))
					&& Math.random() < 0.5
					|| Math.random() < (model.rho)) {
				sDoc.xvec.add(0);
				model.docX[sDoc.index][0]++;
				sDoc.contents.add(new Pair(word, -1));
				model.categoryWord[sDoc.prediction][word]++;
				model.numCWords4Cate[sDoc.prediction]++;
			}
			// nor category word
			else {
				model.docX[sDoc.index][1]++;
				sDoc.xvec.add(1);
				topic = (int) (Math.random() * model.topicNum);
				sDoc.contents.add(new Pair(word, topic));
				model.categoryTopic[sDoc.prediction][topic]++;
				model.docTopic[sDoc.index][topic]++;
				model.wordTopic[word][topic]++;
				model.numRWords4Cate[sDoc.prediction]++;
				model.numRWords4Topic[topic]++;
			}
		}
	}

	// classification with filtering
	private void fakeSeedWordLoadFilter() {
		LuceneHandler lh = new LuceneHandler(model.luceneIndexPath);
		Document[] lucDocs = lh.getDocs();

		int len = model.docNum;
		if (len == 0) {
			System.out.println("there is no document in the path");
			System.exit(-1);
		}
		SDocument sDoc;
		Document luceneDoc;

		for (int i = 0; i < len; i++) {
			model.documents[i] = new SDocument();
			luceneDoc = lucDocs[i];
			sDoc = model.documents[i];

			sDoc.index = i;
			model.kappa[i] = 0.5;
			sDoc.y = 1;
			sDoc.scores = new double[model.cateNum];
			sDoc.title = luceneDoc.get(LuceneHandler.TITLE);
			sDoc.groundTruth = Integer.parseInt(luceneDoc
					.get(LuceneHandler.CATE));
			sDoc.check = Integer.parseInt(luceneDoc.get(LuceneHandler.CHECK)) == 1 ? true
					: false;

			sDoc.xvec = new ArrayList<>();
			if (sDoc.check)
				model.groundTruth[sDoc.groundTruth]++;
			fakeSeedWordInitCont(luceneDoc.get(LuceneHandler.ABSTRACT), sDoc);
		}
	}

	// classification with filtering
	private void fakeSeedWordInitCont(String cont, SDocument sDoc) {

		String[] ves = cont.split(" ");
		double[] re = new double[model.cateNum];

		for (String s : ves) {
			for (int cate = 0; cate < model.iCateNum; cate++)
				if (model.seedSet[cate].contains(s))
					re[cate]++;
			for (int cate = model.iCateNum; cate < model.cateNum; cate++)
				if (model.fakeSeedSet[cate].contains(s))
					re[cate]++;
		}
		initEta(re, sDoc.index);
		sDoc.prediction = (int) Math.floor(Math.random() * model.cateNum);
		if (sDoc.prediction >= model.iCateNum) {
			model.bCateDoc[sDoc.prediction]++;
			model.bDocSum++;
			sDoc.y = 0;
		}
		int word, topic;

		for (String s : ves) {
			word = Dictionary.contains(s);
			if (word == -1)
				continue;
			if (sDoc.prediction < model.iCateNum) {
				// category word
				if (model.seedSet[sDoc.prediction].contains(Dictionary
						.getWord(word))
						&& Math.random() < 0.5
						|| Math.random() < (model.rho)) {
					sDoc.xvec.add(0);
					model.docX[sDoc.index][0]++;
					sDoc.contents.add(new Pair(word, -1));
					model.categoryWord[sDoc.prediction][word]++;
					model.numCWords4Cate[sDoc.prediction]++;
				}
				// nor category word
				else {
					model.docX[sDoc.index][1]++;
					sDoc.xvec.add(1);
					topic = (int) (Math.random() * model.topicNum);
					sDoc.contents.add(new Pair(word, topic));
					model.categoryTopic[sDoc.prediction][topic]++;
					model.docTopic[sDoc.index][topic]++;
					model.wordTopic[word][topic]++;
					model.numRWords4Cate[sDoc.prediction]++;
					model.numRWords4Topic[topic]++;
				}

			} else {
				// category word
				if (model.fakeSeedSet[sDoc.prediction].contains(Dictionary
						.getWord(word))
						&& Math.random() < 0.5
						|| Math.random() < (model.rho)) {
					sDoc.xvec.add(0);
					model.docX[sDoc.index][0]++;
					sDoc.contents.add(new Pair(word, -1));
					model.categoryWord[sDoc.prediction][word]++;
					model.numCWords4Cate[sDoc.prediction]++;
				}
				// nor category word
				else {
					model.docX[sDoc.index][1]++;
					sDoc.xvec.add(1);
					topic = (int) (Math.random() * model.topicNum);
					sDoc.contents.add(new Pair(word, topic));
					model.categoryTopic[sDoc.prediction][topic]++;
					model.docTopic[sDoc.index][topic]++;
					model.wordTopic[word][topic]++;
					model.numRWords4Cate[sDoc.prediction]++;
					model.numRWords4Topic[topic]++;
				}

			}

		}
	}

	private void initEta(double[] raw, int index) {
		int sum = 0;

		for (int i = 0; i < raw.length; i++)
			raw[i] = Math.log(1 + raw[i]);

		for (double d : raw)
			sum += d;
		if (sum == 0) {
			for (int i = 0; i < raw.length; i++)
				raw[i] = soomth;
		} else {
			for (int i = 0; i < raw.length; i++)
				raw[i] = raw[i] / sum;
		}
		model.eta[index] = raw;
	}

	private void initPhi() {
		for (int c = 0; c < model.cateNum; c++)
			for (int k = 0; k < model.topicNum; k++) {
				model.phi[c][k] = (model.categoryTopic[c][k] + model.alpha0)
						/ (model.numRWords4Cate[c] + model.topicNum
								* model.alpha0);
				model.phi[c][k] *= model.alpha2;
				model.phiSum[c] += model.phi[c][k];
			}
	}

	public SModel decorateSModel() {
		if (model.method == 0) {
			System.out.println("loading documents...");
			load();
			initPhi();
		} else if (model.method == 1) {
			System.out.println("loading documents...");
			fakeSeedWordLoadFilter();
			initPhi();

		}
		return model;
	}
}
