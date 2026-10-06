package zio.quill.quotation

import zio.quill.ast.Dynamic
import zio.quill.ast.Property
import zio.quill.ast.Renameable.Fixed
import zio.quill.ast.Visibility.Visible
import zio.quill.base.Spec
import zio.quill.MirrorContexts.testContext.qr1
import zio.quill.MirrorContexts.testContext.qrRegular

class IsDynamicSpec extends Spec {

  "detects if the quotation has dynamic parts" - {
    "true" - {
      "fully dynamic" in {
        IsDynamic(Dynamic(1)) mustEqual true
      }
      "partially dynamic" in {
        IsDynamic(Property(Dynamic(1), "a")) mustEqual true
      }
      "partially dynamic - fixed" in {
        IsDynamic(Property.Opinionated(Dynamic(1), "a", Fixed, Visible)) mustEqual true
      }
    }
    "false" in {
      IsDynamic(qr1.ast) mustEqual false
    }
    "false when using CaseClass" in {
      IsDynamic(qrRegular.ast) mustEqual false
    }
  }
}
