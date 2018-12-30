package dfc;

import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Create by CSQ
 **/

public class SModel {
	protected int topicNum;
	protected int cateNum;// iCateNum+bCateNum
	protected int docNum;
	protected int wordNum;
	protected int numTestSet;
	protected int corpusLength;
	protected int maxDocLen;
	protected int inter;
	protected int report;
	protected int topk;
	protected float rho;
	protected float alpha0;
	protected float alpha2;
	protected float beta0;
	protected float beta1;

	protected String seedwordPath;
	protected String testSetPath;
	protected String trainSetPath;
	protected String luceneIndexPath;
	protected String catalogPath;
	protected String LDAwordmapPath;
	protected String LDAtassignPath;
	protected String LDAphiPath;
	protected String LDAtwordPath;

	protected int[][] wordTopic; // int[word][topic],Ntw
	protected int[][] docTopic; // int[doc][topic],Ndt

	// ********************
	// int[0..iCateNum-1][word] is Ncw
	// int[iCateNum...iCateNum+bCateNum][word] is Nbw
	// ********************
	protected int[][] categoryWord; // int[category][word],Ncw
	protected int[][] categoryTopic; // int[category][topic]£¨Nct
	protected int[][] docX; // int[doc][x],the number of category word in each
							// document

	protected int[] groundTruth; // int[category], the category of each document
	protected int[] numRWords4Topic; // int[topic] number of regular words for
										// one topic,°∆Ntw
	protected int[] numCWords4Cate; // int[category] number of category words
									// for one category,°∆Ncw
	protected int[] numRWords4Cate; // int[category] °∆Nct the total number of
									// words within the doc of category c and
									// the words are not category words)

	protected double[][] phi; // double[category][topic]
	protected double[][] eta; // double[doc][category] £¨¶«d(c)
	protected double[][] tao; // double[word][category]; the relevance weight
								// between word w and category

	protected double[] phiSum;// double[category] °∆phi_c
	protected double[] temp;

	protected SDocument[] documents;
	protected HashSet<String>[] seedSet;

	// ¿©’π±‰¡ø
	protected float alpha1;
	protected double kd;
	protected int bCateNum;
	protected int iCateNum;
	protected int[] bCateDoc;
	protected int bDocSum;
	protected double[] kappa;
	protected double delta_wb;
	protected HashMap[] pwordTopic;// <word,P(w|z)>
	protected HashMap[] pwordCategory;// <word,P(w|c)
	protected HashMap<String, double[]> wordCategory;// <word,xi_wc>the word
														// distribution of
														// category-topic c
	protected HashMap[] topicWord;// <word,phi_wt>
	protected HashSet<String>[] fakeSeedSet;
	protected int fakeSeedNum;
	protected int LDAtopicnum;

	protected double[] threadhold;// double[interesting category]
	protected int btruth;
	protected int ctruth;
	protected int method;
	protected int relMethod;

	protected BufferedWriter resultWriter;

}
