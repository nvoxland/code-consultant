package net.voxland.codeconsultant.appengine;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManagerFactory;

public class PersistenceManagerSingleton {
    private static final PersistenceManagerFactory emfInstance = JDOHelper.getPersistenceManagerFactory("transactions-optional");

    private PersistenceManagerSingleton() {}

    public static PersistenceManagerFactory get() {
        return emfInstance;
    }

}
