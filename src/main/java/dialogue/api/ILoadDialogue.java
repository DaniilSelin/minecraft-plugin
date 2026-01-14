package dialogue.api;

import java.io.IOException;

import dialogue.models.Dialogue;

// Лежит в api, только из за наличия комманды loadDialogs
@FunctionalInterface
public interface ILoadDialogue {
    Dialogue load(String path) throws IOException ;
}