package code.essence.main.client;

import code.essence.utils.client.chat.StringHelper;

import java.io.File;

public record ClientInfo(java.lang.String clientName, java.lang.String userName, java.lang.String role, File clientDir, File filesDir, File configsDir) implements ClientInfoProvider {

    @Override
    public java.lang.String getFullInfo() {
        return java.lang.String.format("Welcome! Client: %s Version: %s Branch: %s", clientName, "читак вроде", StringHelper.getUserRole());
    }
}