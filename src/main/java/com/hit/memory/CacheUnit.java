package com.hit.memory;

import com.hit.algorithm.IAlgoCache;
import com.hit.dao.IDao;
import com.hit.dm.DataModel;

/**
 * @param <T>
 */
public class CacheUnit<T> {
    private IAlgoCache<Long, DataModel<T>> algo;
    private IDao<Long, DataModel<T>> dao;
    private int countSwaps;
    private int countRequests;

    /**
     * @param algo - Cache paging algorithm and manage them
     * @param dao  - Operate as a hard drive
     */
    public CacheUnit(IAlgoCache<Long, DataModel<T>> algo, IDao<Long, DataModel<T>> dao) {
        this.algo = algo;
        this.dao = dao;
        countSwaps = 0;
        countRequests = 0;
    }

    /**
     * @param ids - ids to identify requested values
     * @return DataMadel<T>[] - requested values
     */
    public DataModel<T>[] getDataModels(Long[] ids) {
        DataModel<T>[] dataModelArr = new DataModel[ids.length];
        int i = 0;

        DataModel<T> value;

        for (Long id : ids) {
            value = algo.putElement(id, null);
            countRequests++;
            if (value == null) { //  if - value == null => cache is not full OR the item exist in ALGO =>
                value = algo.getElement(id); //  check if the ID exist add to the ARR.
                countRequests++;
                if (value != null) { // the ID exist in cache
                    dataModelArr[i++] = value;
                } else { //  else the cache not full and you need to retrieve the DM with DAO and put in ALGO.

                    value = retrieveDMFromDAO(id);
                    countRequests++;
                    if (value != null) {
                        dataModelArr[i++] = value;
                    }
                }
            } else { //  else - value != null => cache is full => retrieve the DM with DAO and put the DM to ALGO.
                value = retrieveDMFromDAO(id);
                dataModelArr[i++] = value;
                countRequests++;
            }
        }

        if (dataModelArr[0] == null) {
            dataModelArr = null;
        }

        return dataModelArr;
    }

    /**
     * this will retrieve the DataModel from the DAO and store it in ALGO if needed
     * @param id
     * @return
     */
    private DataModel<T> retrieveDMFromDAO(Long id) {
        DataModel<T> value = dao.find(id);
        DataModel<T> tempValue = null;

        // store the pair (id, value) in cache if necessary
        if (value != null) {
            tempValue = algo.putElement(id, value);
            dao.delete(value);
            
            // store the DataModel (value) in the DAO in case ALGO is full
            if (tempValue != null) {
                countSwaps++;
                dao.save(tempValue);
            }
        }

        return value;
    }

    public int getCountSwaps() {
        return countSwaps;
    }
    
    public int getCountRequest() {
        return countRequests;
    }

    public void deleteDataModelFromMemory(Long id, DataModel<T> entity){
        this.algo.removeElement(id);
        if (entity != null) {
        	this.dao.delete(entity);
        }
    }
}