package zio.quill.dsl

private[quill] trait CoreDsl
    extends InfixDsl
    with OrdDsl
    with QueryDsl
    with QuotationDsl
    with EncodingDsl
    with MetaDsl
    with DynamicQueryDsl
