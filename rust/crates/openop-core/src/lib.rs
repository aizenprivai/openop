//! OPENOP shared core.
//!
//! Plain Rust domain logic. This crate must not depend on UniFFI or any
//! other FFI types; `openop-ffi` converts these types for Kotlin.

#![forbid(unsafe_code)]

/// Version of the core library, taken from the crate manifest.
pub const CORE_VERSION: &str = env!("CARGO_PKG_VERSION");

/// Build profile the core was compiled with.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum BuildProfile {
    Debug,
    Release,
}

impl BuildProfile {
    /// Profile of the current build.
    pub const fn current() -> Self {
        if cfg!(debug_assertions) {
            Self::Debug
        } else {
            Self::Release
        }
    }

    pub const fn as_str(self) -> &'static str {
        match self {
            Self::Debug => "debug",
            Self::Release => "release",
        }
    }
}

/// Errors returned by input validation.
#[derive(Debug, Clone, PartialEq, Eq, thiserror::Error)]
pub enum ValidationError {
    #[error("input is empty")]
    Empty,
}

/// Returns the input unchanged if it contains non-whitespace text.
///
/// Phase 1 uses this only to demonstrate error handling across the
/// Kotlin/Rust boundary.
pub fn validate_non_empty(input: &str) -> Result<&str, ValidationError> {
    if input.trim().is_empty() {
        Err(ValidationError::Empty)
    } else {
        Ok(input)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn core_version_matches_manifest() {
        assert_eq!(CORE_VERSION, env!("CARGO_PKG_VERSION"));
        assert!(!CORE_VERSION.is_empty());
    }

    #[test]
    fn build_profile_matches_debug_assertions() {
        let expected = if cfg!(debug_assertions) {
            "debug"
        } else {
            "release"
        };
        assert_eq!(BuildProfile::current().as_str(), expected);
    }

    #[test]
    fn empty_input_is_rejected() {
        assert_eq!(validate_non_empty(""), Err(ValidationError::Empty));
    }

    #[test]
    fn whitespace_only_input_is_rejected() {
        assert_eq!(validate_non_empty("  \n\t"), Err(ValidationError::Empty));
    }

    #[test]
    fn non_empty_input_is_returned_unchanged() {
        assert_eq!(validate_non_empty("abc"), Ok("abc"));
        assert_eq!(validate_non_empty(" a "), Ok(" a "));
    }
}
