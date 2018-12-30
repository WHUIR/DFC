DFC
====

The java implementation of "Seed-Guided Topic Model for Document Filtering and Classification ", TOIS 2018.
The model DFC proposed in this paper is an effective topic model for dataless text classification and filtering task.
Feel free to contact me if you find any problem in the package.
sqchen@whu.edu.cn 

## Requirements:
* Jre1.7
* Lucene 5.2.1

## Description
#Training/Testing Data Format
Each training sample is a document,which was preprocessed to remove stop words,  and each word is separated by space.
Example: training testing data

#Catelog File
Catalog file is uesd to describe category information. The catalog file of data set '20 news groups' sholud be wrote in this form :
Example:
cate
comp.graphics
cate
sci.med ...
If we want to combine several categories into one , take 'comp' and 'sci' as examples , then the file should be wrote like this :
Example:
cate 
comp.graphics 
comp.os.ms-windows.misc 
comp.sys.ibm.pc.hardware 
comp.windows.x 
comp.sys.mac.hardware
 cate 
sci.crypt 
sci.med 
sci.space 
sci.electronics ...
 
#Note:
The directory “catalog-classification” is for classification task without filtering.The file only contains the categories for classification task.
The directory “catalog-classificationWithFiltering” is for classification with filtering task.The file contains all the categories in dataset.The specifed categories are in the heading, the remaining are irrelevant categories.
Example:
Classification with filtering task: med-space. med and space are specifed categories, others are irrelevant categories.
cate
med
cate
space
cate
sci.crypt
cate 
sci.electronics ...

#Seed Word File
Each line in seed word file corresponds to a category.Seed word is separated by space.Make sure that the category order in catalog file is the same as the order in seed word file.
LDA File
we take the LDA results after running LDA over 100 iterations, using the default parameter setting(Toolkit:JGibbLDA http://jgibblda.sourceforge.net/)

##Parameter Setting
* method:0:classification, 1:classification with filtering 
* relMethod:the method to estimating category word probability, 1:doc-rel, 0:topic-rel        
* iCateNum:the total number of specifed categories/relevant-topics,the variable R in paper
* bCateNum:the setting total number of irrelevant-topics,the variable T in paper
* btruth:the total number of irrelevant-topics, which is only used for evaluation and load documents
* cateNum:the setting total number of categories
* ctruth:the total number of categories, which is only used for 
* fakeSeedNum:the number of pseudo seed words
* topicNum:the total number of general-topics, the variable B in paper
* LDAtopicnum:the number of LDA hidden topic
* alpha0:the variable alpha0 in paper  
* alpha1:the variable alpha1 in paper
* alpha2:the variable alpha1 in paper
* beta0:the variable beta0 in paper
* beta1:the variable beta1 in paper
* rho:the variable rho in paper
* inter:the number of iterations
* report:the method of evaluation, 1:macro F1 , 0:accuracy
* maxDocLen:the max length of documents in the dataset
* kd:the prior likelihood for document d being a relevant document, the variable k_d in paper
* testSetPath:test set path
* trainSetPath:train set path
* catalogPath:category file path
* seedwordPath:seed word file path
* luceneIndexPath:LuceneIndex path
* LDAwordmapPath:LDA word map path
* LDAtassignPath:LDA model path
* LDAphiPath:LDA phi path
* LDAtwordPath:LDA top words path
* resultWriter:output the predict result in file

## aunch the program
The main java entry is in class DfcMain.java.To launch the program there are several parameters must be setting as described above.
If you run the task of classification, you need to set the parameters bCateNum and btruth to be zero, then set the catalogPath

