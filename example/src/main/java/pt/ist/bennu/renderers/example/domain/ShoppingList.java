package pt.ist.bennu.renderers.example.domain;

import org.joda.time.DateTime;

import pt.ist.bennu.core.domain.VirtualHost;

public class ShoppingList extends ShoppingList_Base {
    public ShoppingList() {
        super();
        setCreationDate(new DateTime());
        setHost(VirtualHost.getVirtualHostForThread());
    }
}
