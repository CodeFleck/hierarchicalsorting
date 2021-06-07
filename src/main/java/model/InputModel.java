package model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@AllArgsConstructor
@Builder
@Data
public class InputModel {

    private int propertyQuantity;
    private int metricsQuantity;

}
