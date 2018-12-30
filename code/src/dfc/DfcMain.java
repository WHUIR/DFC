package dfc;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import Util.Dictionary;

/**
 * Created by csq
 */
public class DfcMain {
    public void run()  {

        SModel model = new SModel();
        
        /************
         * parameter setting
         ************/
        
        //0:classification, 1:classification with filtering 
        model.method=1;
        
        //the method to estimating category word probability, 1:doc-rel, 0:topic-rel
        model.relMethod=1;
        
        //the total number of specifed categories/relevant-topics,the variable R in paper
        model.iCateNum=2;
        
        //the setting total number of irrelevant-topics,the variable T in paper. If you run the classification task, this parameter need to be setting as zero
        model.bCateNum=20;
        
        //the total number of irrelevant-topics, which is only used for evaluation and load documents. If you run the classification task, this parameter need to be setting as zero
        model.btruth=18;
        
        //the setting total number of categories
        model.cateNum=model.iCateNum+model.bCateNum;
        
        //the total number of categories, which is only used for evaluation and load documents
        model.ctruth=model.iCateNum+model.btruth;
       
        //the number of pseudo seed words
        model.fakeSeedNum=10;
        
        //the total number of general-topics, the variable B in paper
        model.topicNum = 3* model.cateNum;
        
        //the number of LDA hidden topic
        model.LDAtopicnum=60;
        
        //the variable alpha0 in paper
        model.alpha0 = (float) 50 / model.topicNum;
        //the variable alpha1 in paper
        model.alpha1=(float) 50 / model.bCateNum;
        //the variable alpha2 in paper
        model.alpha2 = 100;
        //the variable beta0 in paper
        model.beta0 = (float) 0.01;
        //the variable beta1 in paper
        model.beta1 = (float) 0.01;
        //the variable rho in paper
        model.rho = 0.95f;
        //the number of iterations
        model.inter = 20; // iterations
        //the method of evaluation, 1:macro F1 , 0:accurac
        model.report = 1;
        //the max length of documents in the dataset
        model.maxDocLen = 40000; // max document length
        //the prior likelihood for document d being a relevant document, the variable k_d in paper
        model.kd=0.5;
        
        //test set path
        model.testSetPath = ".\\DataSet-20ng\\Test-testset";
        //train set path
        model.trainSetPath = ".\\DataSet-20ng\\Test-trainset";
        //category file path
        model.catalogPath = ".\\catalog-classificationWithFiltering\\med-space";
        //seed word file path
        model.seedwordPath = ".\\seedword\\SD\\med-space";
        //LuceneIndex path
        model.luceneIndexPath = ".\\luceneIndex"; 
        //LDA word map path
        model.LDAwordmapPath=".//lda-20ng-60//wordmap.txt";
        //LDA model path
        model.LDAtassignPath=".//lda-20ng-60//model-final.tassign";
        //LDA phi path
        model.LDAphiPath=".//lda-20ng-60//model-final.phi";
        //LDA top word path
        model.LDAtwordPath=".//lda-20ng-60//model-final.twords";
        
        
        //output the predict result in file
        try {
			model.resultWriter=new BufferedWriter(new FileWriter("./testout.txt",true));
		} catch (IOException e) {
			e.printStackTrace(); 
		}

        //index the corpus
        new LuceneIndex(model).decorateSModel();

        // initialize the variates
        new Initialize(model).decorateSModel();
        
        //calculate the relevant weight between seed word and category
        new Co_occurrence(model).decorateSModel();

        //load the documents
        new LoadDocs(model).decorateSModel();
        
        //predict the category label of documents
        new Predict(model).decorateSModel();
        
        
    }
    
    //runner
    public static void main(String args[]){
    	DfcMain dfc=new DfcMain();
    	dfc.run();
    }
}
