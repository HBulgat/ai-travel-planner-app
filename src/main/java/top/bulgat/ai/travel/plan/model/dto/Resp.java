package top.bulgat.ai.travel.plan.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Resp<T> implements Serializable {
    private int code;
    private String message;
    private T data;
}
