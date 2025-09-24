package org.teletronics.vsyrov.filestorage.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.teletronics.vsyrov.filestorage.common.model.FileMetadata;

/**
 * @author vsyrov
 */
@Mapper(componentModel = "spring")
public interface FileMapper {

//    @Mapping(target = "visibility", expression = "java( com.example.filestorage.api.model.Visibility.fromValue( file.getVisibility().name() ) )")
//    FileDto toDto(FileMetadata file);
}
