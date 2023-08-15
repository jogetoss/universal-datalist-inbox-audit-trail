package org.joget.marketplace;

import org.joget.apps.app.service.AppUtil;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;

public class AppContext {

    private static AppContext instance;
    private final GenericXmlApplicationContext springApplicationContext;

    public synchronized static AppContext getInstance() {
        if (instance == null) {
            instance = new AppContext();
        }
        return instance;
    }

    private AppContext() {
        this.springApplicationContext = new GenericXmlApplicationContext();
        this.springApplicationContext.setValidating(false);
        this.springApplicationContext.setClassLoader(this.getClass().getClassLoader());
        this.springApplicationContext.setParent(AppUtil.getApplicationContext());
        this.springApplicationContext.load("/assignmentApplicationContext.xml");
        this.springApplicationContext.refresh();
    }

    public AbstractApplicationContext getAppContext() {
        return springApplicationContext;
    }

}
