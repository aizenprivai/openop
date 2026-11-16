//! UniFFI exports of the OPENOP core.
//!
//! This is the only crate that knows about UniFFI. It converts
//! `openop-core` types into FFI types and keeps the boundary narrow.
//! Rules (see docs/phase-1-plan.md §5.3):
//! - every fallible export returns `Result<_, CoreError>`;
//! - never log user content.

uniffi::setup_scaffolding!();

use openop_core::{BuildProfile, CORE_VERSION, ValidationError};

/// Basic information about the Rust core, for display and diagnostics.
#[derive(Debug, Clone, PartialEq, Eq, uniffi::Record)]
pub struct CoreInfo {
    /// Core library version (crate version).
    pub core_version: String,
    /// "debug" or "release".
    pub build_profile: String,
}

/// Errors the core reports to Kotlin.
#[derive(Debug, Clone, PartialEq, Eq, thiserror::Error, uniffi::Error)]
pub enum CoreError {
    #[error("invalid input: {reason}")]
    InvalidInput { reason: String },
}

impl From<ValidationError> for CoreError {
    fn from(err: ValidationError) -> Self {
        Self::InvalidInput {
            reason: err.to_string(),
        }
    }
}

/// Returns the core version and build profile.
#[uniffi::export]
pub fn core_info() -> CoreInfo {
    CoreInfo {
        core_version: CORE_VERSION.to_owned(),
        build_profile: BuildProfile::current().as_str().to_owned(),
    }
}

/// Returns `input` unchanged, or `CoreError::InvalidInput` if it is empty.
///
/// Phase 1 demonstration of error handling across the boundary.
#[uniffi::export]
pub fn echo_validated(input: String) -> Result<String, CoreError> {
    openop_core::validate_non_empty(&input)?;
    Ok(input)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn core_info_reports_crate_version() {
        let info = core_info();
        assert_eq!(info.core_version, env!("CARGO_PKG_VERSION"));
        assert!(info.build_profile == "debug" || info.build_profile == "release");
    }

    #[test]
    fn echo_validated_returns_input() {
        assert_eq!(echo_validated("abc".into()), Ok("abc".into()));
    }

    #[test]
    fn echo_validated_maps_empty_input_to_error() {
        assert_eq!(
            echo_validated(String::new()),
            Err(CoreError::InvalidInput {
                reason: "input is empty".into()
            })
        );
    }
}
