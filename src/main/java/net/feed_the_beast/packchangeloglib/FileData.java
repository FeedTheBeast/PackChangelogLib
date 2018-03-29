package net.feed_the_beast.packchangeloglib;

import addons.curse.AddOnFile;
import lombok.Data;
import org.datacontract.schemas._2004._07.curse_addonservice_requests.AddOnFileKey;

import java.util.List;

@Data
public class FileData {
    private List<AddOnFile> files;
    private AddOnFileKey[] mappings;
}
