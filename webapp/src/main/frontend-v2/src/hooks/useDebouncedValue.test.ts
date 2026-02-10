import { act, renderHook } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { useDebouncedValue } from "./useDebouncedValue";

describe("useDebouncedValue", () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  it("should return value immediately", () => {
    const { result } = renderHook(() => useDebouncedValue("initial"));

    expect(result.current).toBe("initial");
  });

  it("should debounce value changes with default delay", () => {
    const { result, rerender } = renderHook(
      ({ value }) => useDebouncedValue(value),
      { initialProps: { value: "initial" } },
    );

    expect(result.current).toBe("initial");

    rerender({ value: "updated" });
    expect(result.current).toBe("initial");

    act(() => {
      vi.advanceTimersByTime(299);
    });
    expect(result.current).toBe("initial");

    act(() => {
      vi.advanceTimersByTime(1);
    });
    expect(result.current).toBe("updated");
  });

  it("should debounce value changes with custom delay", () => {
    const { result, rerender } = renderHook(
      ({ value, delay }) => useDebouncedValue(value, delay),
      { initialProps: { value: "initial", delay: 500 } },
    );

    expect(result.current).toBe("initial");

    rerender({ value: "updated", delay: 500 });
    expect(result.current).toBe("initial");

    act(() => {
      vi.advanceTimersByTime(499);
    });
    expect(result.current).toBe("initial");

    act(() => {
      vi.advanceTimersByTime(1);
    });
    expect(result.current).toBe("updated");
  });

  it("should reset debounce timer on rapid value changes", () => {
    const { result, rerender } = renderHook(
      ({ value }) => useDebouncedValue(value, 300),
      { initialProps: { value: "initial" } },
    );

    rerender({ value: "first" });
    act(() => {
      vi.advanceTimersByTime(100);
    });
    expect(result.current).toBe("initial");

    rerender({ value: "second" });
    act(() => {
      vi.advanceTimersByTime(100);
    });
    expect(result.current).toBe("initial");

    rerender({ value: "final" });
    act(() => {
      vi.advanceTimersByTime(300);
    });
    expect(result.current).toBe("final");
  });

  it("should handle multiple sequential updates", () => {
    const { result, rerender } = renderHook(
      ({ value }) => useDebouncedValue(value, 100),
      { initialProps: { value: "initial" } },
    );

    rerender({ value: "update1" });
    act(() => {
      vi.advanceTimersByTime(100);
    });
    expect(result.current).toBe("update1");

    rerender({ value: "update2" });
    act(() => {
      vi.advanceTimersByTime(100);
    });
    expect(result.current).toBe("update2");
  });

  it("should work with different data types", () => {
    const { result, rerender } = renderHook(
      ({ value }) => useDebouncedValue(value, 100),
      { initialProps: { value: 42 } },
    );

    expect(result.current).toBe(42);

    rerender({ value: 84 });
    act(() => {
      vi.advanceTimersByTime(100);
    });
    expect(result.current).toBe(84);
  });

  it("should work with object values", () => {
    const initialObj = { id: 1, name: "initial" };
    const updatedObj = { id: 2, name: "updated" };

    const { result, rerender } = renderHook(
      ({ value }) => useDebouncedValue(value, 100),
      { initialProps: { value: initialObj } },
    );

    expect(result.current).toBe(initialObj);

    rerender({ value: updatedObj });
    act(() => {
      vi.advanceTimersByTime(100);
    });
    expect(result.current).toBe(updatedObj);
  });

  it("should handle null and undefined values", () => {
    type TestValue = undefined | null | string;
    type TestValueProps = { value: undefined | null | string };
    const { result, rerender } = renderHook<TestValue, TestValueProps>(
      ({ value }) => {
        return useDebouncedValue(value, 100);
      },
      { initialProps: { value: null } },
    );

    expect(result.current).toBeNull();

    rerender({ value: undefined });
    act(() => {
      vi.advanceTimersByTime(100);
    });
    expect(result.current).toBeUndefined();

    rerender({ value: "not null" });
    act(() => {
      vi.advanceTimersByTime(100);
    });
    expect(result.current).toBe("not null");
  });

  it("should clean up timeout on unmount", () => {
    const clearTimeoutSpy = vi.spyOn(globalThis, "clearTimeout");

    const { unmount, rerender } = renderHook(
      ({ value }) => useDebouncedValue(value, 100),
      { initialProps: { value: "initial" } },
    );

    rerender({ value: "updated" });

    unmount();

    expect(clearTimeoutSpy).toHaveBeenCalled();
  });

  it("should handle delay changes", () => {
    const { result, rerender } = renderHook(
      ({ value, delay }) => useDebouncedValue(value, delay),
      { initialProps: { value: "initial", delay: 300 } },
    );

    rerender({ value: "updated", delay: 100 });

    act(() => {
      vi.advanceTimersByTime(100);
    });
    expect(result.current).toBe("updated");
  });

  it("should handle zero delay", () => {
    const { result, rerender } = renderHook(
      ({ value }) => useDebouncedValue(value, 0),
      { initialProps: { value: "initial" } },
    );

    rerender({ value: "updated" });

    act(() => {
      vi.advanceTimersByTime(0);
    });
    expect(result.current).toBe("updated");
  });

  it("should maintain referential equality for unchanged values", () => {
    const obj = { test: "value" };
    const { result, rerender } = renderHook(
      ({ value }) => useDebouncedValue(value, 100),
      { initialProps: { value: obj } },
    );

    const firstResult = result.current;

    // Rerender with same object reference
    rerender({ value: obj });

    act(() => {
      vi.advanceTimersByTime(100);
    });

    expect(result.current).toBe(firstResult);
  });
});
