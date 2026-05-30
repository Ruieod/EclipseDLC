

package code.essence.utils.client.managers.api.command.argument;

import code.essence.utils.client.managers.api.command.argparser.IArgParser;
import code.essence.utils.client.managers.api.command.exception.CommandInvalidTypeException;


public interface ICommandArgument {

    
    int getIndex();

    
    String getValue();

    
    String getRawRest();

    
    <E extends Enum<?>> E getEnum(Class<E> enumClass) throws CommandInvalidTypeException;

    
    <T> T getAs(Class<T> type) throws CommandInvalidTypeException;

    
    <T> boolean is(Class<T> type);

    
    <T, S> T getAs(Class<T> type, Class<S> stateType, S state) throws CommandInvalidTypeException;

    
    <T, S> boolean is(Class<T> type, Class<S> stateType, S state);
}
