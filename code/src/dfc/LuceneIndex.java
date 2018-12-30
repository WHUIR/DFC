package dfc;
import Util.LuceneWriter;

/**
 * Created by csq.
 */
public class LuceneIndex implements Decorator {

    private SModel model;


    public LuceneIndex(SModel model) {
        this.model = model;
    }

    private void luceneIndex(){
        LuceneWriter.writeIndex(model.luceneIndexPath,model.testSetPath,model.ctruth,model.catalogPath,true);
        LuceneWriter.writeIndex_append(model.luceneIndexPath, model.trainSetPath, model.ctruth, model.catalogPath, false);
    }

    @Override
    public SModel decorateSModel() {
        luceneIndex();
        return model;
    }
}
