package load;

import java.io.IOException;

@FunctionalInterface
public interface ILoad<T> {
    T load(String path) throws IOException ;
}