package pt.ist.bennu.renderers.example.domain;

import java.util.TreeSet;

import pt.ist.bennu.core.domain.VirtualHost;
import pt.ist.fenixframework.Atomic;

public class Product extends Product_Base implements Comparable<Product> {

    public Product() {
        super();
        setHost(VirtualHost.getVirtualHostForThread());
    }

    public Product(String name) {
        this();
        setName(name);
    }

    public static TreeSet<Product> getPossibleProducts() {
        if (VirtualHost.getVirtualHostForThread().getProductSet().isEmpty()) {
            initProducts();
        }
        return new TreeSet<>(VirtualHost.getVirtualHostForThread().getProductSet());
    }

    @Atomic
    private static void initProducts() {
        new Product("Maçã");
        new Product("Banana");
        new Product("Bróculos");
        new Product("Tomate");
    }

    @Override
    public int compareTo(Product o) {
        return getName().compareTo(o.getName());
    }
}
