package org.teletronics.vsyrov.filestorage.api.mapper;

import org.mapstruct.Mapper;
import org.teletronics.vsyrov.filestorage.api.model.FileDto;
import org.teletronics.vsyrov.filestorage.common.model.FileMetadata;

/**
 * @author vsyrov
 */
@Mapper(componentModel = "spring")
public interface FileMapper {
    FileDto toDto(FileMetadata m);
}
