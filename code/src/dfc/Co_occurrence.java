package dfc;

import Util.Dictionary;
import Util.LuceneHandler;
import Util.paper.index.PaperAbsIndexHandler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.document.Document;

/**
 * Created by Csq
 */
public class Co_occurrence implements Decorator {

	private static HashMap<String, HashSet<String>> word_doc;
	private static PaperAbsIndexHandler handler;
	public SModel model;
	private static HashSet<Integer> interword = new HashSet<Integer>();
	private static HashMap<String, Integer> LDAword2id = new HashMap<String, Integer>();
	private static HashMap<Integer, String> LDAid2word = new HashMap<Integer, String>();
	private HashMap<Integer, Double>[] LDApks = new HashMap[30];
	private static int topk;
	private HashMap<Integer, Double>[] LDAtoppks = new HashMap[30];
	private static int LDAtopicnum;
	private static int filtertopicnum;
	private static double LDAalpha;
	private double[] LDApz;
	private double[][] LDAphi;

	private HashMap<Integer, LinkedList<String>> topWord4Topic = new HashMap<Integer, LinkedList<String>>();// HashMap<topic,topWord_List>LDA每个topic下的top-10个词
	private HashMap<Integer, Double> LDAtopicCateRel = new HashMap<Integer, Double>();
	private double[] wordFre;

	public Co_occurrence(SModel model) {
		this.model = model;
		LDAtopicnum = model.LDAtopicnum;
		filtertopicnum = 0;
		LDAalpha = 50.0 / (double) LDAtopicnum;
		LDApz = new double[LDAtopicnum];
		LDAphi = new double[LDAtopicnum][52761];
		wordFre = new double[model.wordNum];
		topk = model.topk;
	}

	/*******
	 * use LDA ******** use LDA to compute delta_{w,c} rel(w,c)=∑s∑k
	 * P_lda(w|k)P_lda(k|s) P_lda(k|s)=P_lda(z=k)P_lda(s|k)/∑k
	 * P_lda(z=i)P_lda(s|i) P_lda(z=k)=(nz[k] + alpha) / (∑k nz[i] + numTopic *
	 * alpha);
	 */
	public void initLDA(String wordmap_path, String tassign_path,
			String phi_path, String topWord_path) {
		String line;
		String[] ves;
		String[] term;
		String[] tmp;
		try {
			BufferedReader br = new BufferedReader(new FileReader(topWord_path));
			int topicid = 0;
			int topword = 0;
			while ((line = br.readLine()) != null) {
				ves = line.split(" ");
				// title
				if (ves[0].equals("Topic")) {
					Pattern p = Pattern.compile("[^0-9]");
					Matcher m = p.matcher(ves[1]);
					topicid = Integer.parseInt(m.replaceAll("").trim());
					topWord4Topic.put(topicid, new LinkedList<String>());
				} else {
					tmp = line.split("\t");
					ves = tmp[1].split(" ");
					topWord4Topic.get(topicid).add(ves[0]);
				}
			}
			br.close();

			char[] c;
			int isNotWord = 0;
			int isNum = 0;
			String word = "";

			for (int k = 0; k < LDAtopicnum; k++) {
				isNum = 0;
				isNotWord = 0;
				for (int w = 0; w < 10; w++) {
					word = topWord4Topic.get(k).get(w);
					if (isNumber(word) == 1)
						isNum++;
					c = word.toCharArray();
					if (c.length < 4)
						isNotWord++;
				}
				if (isNum >= 5 || isNotWord >= 5) {
					topWord4Topic.remove(k);
				}
			}
			filtertopicnum = topWord4Topic.size();

			br = new BufferedReader(new FileReader(wordmap_path));
			br.readLine();
			while ((line = br.readLine()) != null) {
				ves = line.split(" ");
				LDAword2id.put(ves[0], Integer.valueOf(ves[1]));
				LDAid2word.put(Integer.valueOf(ves[1]), ves[0]);
			}
			br.close();

			br = new BufferedReader(new FileReader(tassign_path));
			while ((line = br.readLine()) != null) {
				ves = line.split(" ");
				for (String s : ves) {

					term = s.split(":");
					if (term.length < 2)
						continue;
					LDApz[Integer.valueOf(term[1])]++;
				}
			}
			br.close();

			double sum = 0.0;
			for (Entry<Integer, LinkedList<String>> entry : topWord4Topic
					.entrySet()) {
				sum += LDApz[entry.getKey()];
			}

			for (Entry<Integer, LinkedList<String>> entry : topWord4Topic
					.entrySet()) {
				LDApz[entry.getKey()] = 1.0
						* (LDApz[entry.getKey()] + LDAalpha)
						/ (sum + filtertopicnum * LDAalpha);
			}

			br = new BufferedReader(new FileReader(phi_path));
			int topic = 0;
			while ((line = br.readLine()) != null) {
				ves = line.split(" ");
				for (int w = 0; w < ves.length; w++) {
					LDAphi[topic][w] = Double.valueOf(ves[w]);
				}
				topic++;
			}

		} catch (Exception e) {

			e.printStackTrace();
		}

		for (int i = 0; i < 30; i++) {
			LDApks[i] = new HashMap<Integer, Double>();
			LDAtoppks[i] = new HashMap<Integer, Double>();
		}
		System.out.println(" End load LDA message");

	}

	public int isNumber(String word) {
		Pattern p = Pattern.compile("[0-9][0-9]*");
		Matcher isNum = p.matcher(word);
		if (!isNum.matches()) {
			return 0;
		}
		return 1;
	}

	/*
	 * compute p(k|s)
	 */
	public void computePks(HashSet<String> seedSet) {
		double sum = 0.0;
		double tmp[] = new double[LDAtopicnum];
		List<Map.Entry<Integer, Double>> maplist = null;

		int i = 0;
		for (String s : seedSet) {

			if (LDAword2id.get(s) == null) {
				System.out.println(String.format(
						"word %s is not found in the vocabulary", s));
				continue;
			}

			for (Entry<Integer, LinkedList<String>> entry : topWord4Topic
					.entrySet()) {
				tmp[entry.getKey()] = LDApz[entry.getKey()]
						* LDAphi[entry.getKey()][LDAword2id.get(s)];
				sum = sum + tmp[entry.getKey()];
			}

			for (Entry<Integer, LinkedList<String>> entry : topWord4Topic
					.entrySet()) {
				LDApks[i].put(entry.getKey(), tmp[entry.getKey()] / sum);
			}

			maplist = new ArrayList<Map.Entry<Integer, Double>>(
					LDApks[i].entrySet());
			Collections.sort(maplist,
					new Comparator<Map.Entry<Integer, Double>>() {
						public int compare(Map.Entry<Integer, Double> mapping1,
								Map.Entry<Integer, Double> mapping2) {
							return mapping2.getValue().compareTo(
									mapping1.getValue());
						}
					});

			for (int k = 0; k < topk; k++) {
				LDAtoppks[i].put(maplist.get(k).getKey(), maplist.get(k)
						.getValue());
			}

			i++;
		}

	}

	// compute rel(w,c)
	public double computeRel(HashSet<String> seedSet, String word) {
		double rel = 0.0;
		int topic;
		int wordid;
		for (int s = 0; s < seedSet.size(); s++) {
			for (Map.Entry<Integer, Double> entry : LDAtoppks[s].entrySet()) {
				topic = entry.getKey();
				wordid = LDAword2id.get(word);
				rel = rel + LDAphi[entry.getKey()][LDAword2id.get(word)]
						* entry.getValue();
			}
		}
		rel = rel / seedSet.size();
		return rel;
	}

	/*
	 * compute p(w,r)
	 */
	public double compTopicCateRel(HashSet<String>[] seedSet, String word,
			int topicid) {
		int topic = 0;
		int wordid = 0;
		double rel = 0.0;

		for (int r = 0; r < seedSet.length; r++) {

			for (int i = 0; i < LDApks.length; i++) {
				LDApks[i].clear();
				LDAtoppks[i].clear();
			}

			computePks(seedSet[r]);
			for (int s = 0; s < seedSet[r].size(); s++) {
				wordid = LDAword2id.get(word);
				rel = rel + LDAphi[topicid][LDAword2id.get(word)]
						* LDApks[s].get(topicid);
			}
		}
		return rel;
	}

	protected void initwcRelation() {
		double[] sum = new double[model.cateNum];
		double thr = (double) 1 / model.cateNum;
		double temp;
		double[] dev = new double[model.cateNum];
		double[] mean = new double[model.cateNum];
		double avg = 0.0;
		double max = 0.0;
		double[][] exp = new double[model.wordNum][model.cateNum];

		// load LDA file
		initLDA(model.LDAwordmapPath, model.LDAtassignPath, model.LDAphiPath,
				model.LDAtwordPath);

		// create the pesudo seed words of background categories
		/*
		 * p(k,r)=∑w p(w,r),p(w,r)=∑s p(w,s)
		 * p(w,s)=∑k p(w|k)p(k|s)
		 */
		double rel = 0.0;
		for (Entry<Integer, LinkedList<String>> entry : topWord4Topic
				.entrySet()) {
			rel = 0.0;
			for (int w = 0; w < 10; w++) {
				rel = rel
						+ compTopicCateRel(model.seedSet,
								topWord4Topic.get(entry.getKey()).get((w)),
								entry.getKey());
			}
			LDAtopicCateRel.put(entry.getKey(), rel);
		}
		List<Map.Entry<Integer, Double>> maplist = null;
		maplist = new ArrayList<Map.Entry<Integer, Double>>(
				LDAtopicCateRel.entrySet());
		Collections.sort(maplist, new Comparator<Map.Entry<Integer, Double>>() {
			public int compare(Map.Entry<Integer, Double> mapping1,
					Map.Entry<Integer, Double> mapping2) {
				return mapping1.getValue().compareTo(mapping2.getValue());
			}
		});
		int top = 0;
		int topic = 0;
		for (int c = model.iCateNum; c < model.cateNum; c++) {
			topic = maplist.get(top).getKey();
			for (int w = 0; w < model.fakeSeedNum; w++) {
				model.fakeSeedSet[c].add(topWord4Topic.get(topic).get(w));
			}
			top++;
		}

		/**************************
		 * Topic-rel
		 **************************/
		if (model.relMethod == 0) {
			for (int c = 0; c < model.iCateNum; c++) {
				for (int i = 0; i < LDApks.length; i++) {
					LDApks[i].clear();
					LDAtoppks[i].clear();
				}
				computePks(model.seedSet[c]);
				for (int w = 0; w < model.wordNum; w++) {
					model.tao[w][c] = computeRel(model.seedSet[c],
							Dictionary.getWord(w));
					sum[c] += model.tao[w][c];
				}

			}

			for (int c = model.iCateNum; c < model.cateNum; c++) {
				for (int i = 0; i < LDApks.length; i++) {
					LDApks[i].clear();
				}
				computePks(model.fakeSeedSet[c]);
				for (int w = 0; w < model.wordNum; w++) {
					model.tao[w][c] = computeRel(model.fakeSeedSet[c],
							Dictionary.getWord(w));
					sum[c] += model.tao[w][c];
				}
			}
		} 
		/*******************
		 * Doc-rel
		 *******************/
		else if (model.relMethod == 1) {

			for (int i = 0; i < model.wordNum; i++) {
				for (int c = 0; c < model.iCateNum; c++) {
					model.tao[i][c] = Co_occurrence.catecoRelation(
							model.seedSet[c], Dictionary.getWord(i));
					sum[c] += model.tao[i][c];
				}
			}
			for (int i = 0; i < model.wordNum; i++) {
				for (int c = model.iCateNum; c < model.cateNum; c++) {
					model.tao[i][c] = Co_occurrence.catecoRelation(
							model.fakeSeedSet[c], Dictionary.getWord(i));
					sum[c] += model.tao[i][c];
				}
			}
		}

		for (int i = 0; i < model.wordNum; i++) {
			for (int c = 0; c < model.cateNum; c++) {
				model.tao[i][c] /= sum[c];
				mean[c] += model.tao[i][c];
			}
		}
		for (int c = 0; c < model.cateNum; c++) {
			mean[c] = mean[c] / model.wordNum;
		}

		for (int w = 0; w < model.wordNum; w++) {
			for (int c = 0; c < model.cateNum; c++) {
				dev[c] += Math.pow((model.tao[w][c] - mean[c]), 2);
			}
		}
		for (int c = 0; c < model.cateNum; c++) {
			dev[c] = Math.sqrt(dev[c] / model.wordNum);
		}
		for (int w = 0; w < model.wordNum; w++) {
			for (int c = 0; c < model.cateNum; c++) {
				temp = (model.tao[w][c] - mean[c]) / dev[c];
				exp[w][c] = 1 / (1 + Math.exp((-1) * temp));
			}
		}

		// compute tao
		for (int i = 0; i < model.wordNum; i++) {
			temp = 0;
			for (int c = 0; c < model.cateNum; c++) {
				temp += model.tao[i][c];
			}

			if (temp == 0) {
				for (int c = 0; c < model.cateNum; c++) {
					model.tao[i][c] = (double) 1 / Math.pow(model.cateNum, 2);
					model.tao[i][c] = model.tao[i][c] * exp[i][c];
				}
			} else {
				for (int c = 0; c < model.cateNum; c++) {
					model.tao[i][c] /= temp;
					model.tao[i][c] = model.tao[i][c] - thr > 0 ? model.tao[i][c]
							- thr
							: 0;
					model.tao[i][c] = model.tao[i][c] * exp[i][c];
				}
			}
		}

		// compute delta
		double[] cate_avg = new double[model.cateNum];
		for (int i = 0; i < model.wordNum; i++) {
			for (int c = 0; c < model.cateNum; c++) {
				if (model.rho == 1 && model.tao[i][c] == 0) {
					model.tao[i][c] = 1;
				}
				model.tao[i][c] = model.tao[i][c] * model.rho
						/ (1 - model.rho + model.tao[i][c] * model.rho);
			}
		}
	}

	protected void initCoo() {
		try {
			handler = new PaperAbsIndexHandler(model.luceneIndexPath);
			word_doc = new HashMap<>();
		} catch (Exception e) {
			System.out.println("lucene Index Path is illegal");
			e.printStackTrace();
			System.exit(-1);
		}

		Set<String> termVec;
		Set<String> word_docList;
		Map<String, Integer> map;
		try {
			for (int i = 0; i < model.docNum; i++) {

				map = handler.getTermVector(i);
				if (map == null)
					continue;
				else
					termVec = map.keySet();
				for (String s : termVec) {
					if (!word_doc.containsKey(s))
						word_doc.put(s, new HashSet<String>());
					word_docList = word_doc.get(s);
					word_docList.add(handler.getIDToTitle(i));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static int intersection(String seedWord, String word) {
		HashSet<String> l, s;
		if (!word_doc.containsKey(seedWord)) {
			return 0;
		}
		if (!word_doc.containsKey(word)) {
			return 0;
		}
		int re = 0;
		if (word_doc.get(word).size() > word_doc.get(seedWord).size()) {
			l = word_doc.get(word);
			s = word_doc.get(seedWord);
		} else {
			l = word_doc.get(seedWord);
			s = word_doc.get(word);
		}
		for (String title : s) {
			if (l.contains(title))
				re++;
		}
		return re;
	}

	// p(w|s)=df(w,s)/df(s)
	private static double is_un(String seedWord, String word) {
		int wordid;
		if (intersection(seedWord, word) == 0)
			return 0;
		interword.add(Dictionary.getIndex(word));
		double re = (double) intersection(seedWord, word)
				/ word_doc.get(seedWord).size();
		return re;
	}

	public static double catecoRelation(HashSet<String> seedset, String word) {
		double re = 0;
		for (String s : seedset) {
			re += is_un(s, word);
		}
		if (re == 0)
			return 0;
		re /= seedset.size();
		return re;
	}

	@Override
	public SModel decorateSModel() {
		System.out.println("calculate co-occurrence...");
		initCoo();
		initwcRelation();
		return model;
	}
}
