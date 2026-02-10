package Resume.model;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class Resume {

    private UUID id;
    private UUID userId;
    private String originalFileName;
    private String status;
}