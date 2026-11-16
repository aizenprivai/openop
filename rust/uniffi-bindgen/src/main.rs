//! Runs the UniFFI binding generator at the exact `uniffi` version
//! pinned in the workspace, so bindings always match the runtime.

fn main() {
    uniffi::uniffi_bindgen_main()
}
